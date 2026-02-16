import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as sqs from 'aws-cdk-lib/aws-sqs';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import { Construct } from 'constructs';

export class InfracdkStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // VPC para la infraestructura de Fleet
    const vpc = new ec2.Vpc(this, 'FleetVPC', {
      maxAzs: 2,
      natGateways: 1,
    });

    // S3 privado
    const documentsBucket = new s3.Bucket(this, 'DocumentsBucket', {
      bucketName: 'fleet-documents',
      encryption: s3.BucketEncryption.S3_MANAGED,
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      versioned: true,
      lifecycleRules: [{
        expiration: cdk.Duration.days(90)
      }],
      removalPolicy: cdk.RemovalPolicy.RETAIN,
    });

    // Credenciales de base de datos en Secrets Manager
    const dbCredentials = new secretsmanager.Secret(this, 'DBCredentials', {
      secretName: 'fleet-db-credentials',
      generateSecretString: {
        secretStringTemplate: JSON.stringify({ username: 'postgres' }),
        generateStringKey: 'password',
        excludePunctuation: true,
        includeSpace: false,
      },
    });

    // Base de datos Aurora Postgres Serverless v2
    const db = new rds.DatabaseCluster(this, 'SolicitudesDB', {
      engine: rds.DatabaseClusterEngine.auroraPostgres({
        version: rds.AuroraPostgresEngineVersion.VER_15_4
      }),
      credentials: rds.Credentials.fromSecret(dbCredentials),
      defaultDatabaseName: 'fleet_management',
      vpc: vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      writer: rds.ClusterInstance.serverlessV2('writer', {
        publiclyAccessible: false,
      }),
      readers: [
        rds.ClusterInstance.serverlessV2('reader', {
          scaleWithWriter: true,
        }),
      ],
      serverlessV2MinCapacity: 0.5,
      serverlessV2MaxCapacity: 2,
      backup: {
        retention: cdk.Duration.days(7),
      },
      removalPolicy: cdk.RemovalPolicy.SNAPSHOT,
    });

    // SQS
    const SQS_queue = new sqs.Queue(this, 'SolicitudesQueue', {
      encryption: sqs.QueueEncryption.KMS_MANAGED,
      visibilityTimeout: cdk.Duration.seconds(300),
      retentionPeriod: cdk.Duration.days(14),
    });

    // ECS Cluster
    const cluster = new ecs.Cluster(this, 'FleetCluster', {
      vpc,
      containerInsights: true,
    });
    
    const taskDef = new ecs.FargateTaskDefinition(this, 'ServiceTask', {
      memoryLimitMiB: 2048,
      cpu: 512
    });

    // IAM: Principio de mínimo privilegio
    documentsBucket.grantReadWrite(taskDef.taskRole);
    SQS_queue.grantSendMessages(taskDef.taskRole);
    dbCredentials.grantRead(taskDef.taskRole);
    
    // Container con puerto expuesto
    const container = taskDef.addContainer('AppContainer', {
      image: ecs.ContainerImage.fromRegistry('public.ecr.aws/docker/library/nginx:latest'),
      environment: {
        DB_HOST: db.clusterEndpoint.hostname,
        DB_PORT: db.clusterEndpoint.port.toString(),
        DB_NAME: 'fleet_management',
        S3_BUCKET: documentsBucket.bucketName,
        SQS_QUEUE_URL: SQS_queue.queueUrl,
        AWS_REGION: cdk.Stack.of(this).region,
      },
      secrets: {
        DB_USERNAME: ecs.Secret.fromSecretsManager(dbCredentials, 'username'),
        DB_PASSWORD: ecs.Secret.fromSecretsManager(dbCredentials, 'password'),
      },
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: 'fleet-service' }),
      healthCheck: {
        command: ['CMD-SHELL', 'curl -f http://localhost:8080/actuator/health || exit 1'],
        interval: cdk.Duration.seconds(30),
        timeout: cdk.Duration.seconds(5),
        retries: 3,
        startPeriod: cdk.Duration.seconds(60),
      },
    });

    container.addPortMappings({
      containerPort: 8080,
      protocol: ecs.Protocol.TCP,
    });

    // Application Load Balancer
    const alb = new elbv2.ApplicationLoadBalancer(this, 'FleetALB', {
      vpc,
      internetFacing: true,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
    });

    const listener = alb.addListener('HttpListener', {
      port: 80,
      open: true,
    });

    // Fargate Service con ALB
    const service = new ecs.FargateService(this, 'Service', {
      cluster,
      taskDefinition: taskDef,
      desiredCount: 2,
      assignPublicIp: false,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      healthCheckGracePeriod: cdk.Duration.seconds(60),
    });

    // Target Group y registro en ALB
    listener.addTargets('FleetTarget', {
      port: 8080,
      protocol: elbv2.ApplicationProtocol.HTTP,
      targets: [service],
      healthCheck: {
        path: '/actuator/health',
        interval: cdk.Duration.seconds(30),
        timeout: cdk.Duration.seconds(5),
        healthyThresholdCount: 2,
        unhealthyThresholdCount: 3,
      },
      deregistrationDelay: cdk.Duration.seconds(30),
    });

    // Permitir tráfico del ALB al servicio
    service.connections.allowFrom(
      alb,
      ec2.Port.tcp(8080),
      'Allow traffic from ALB'
    );

    // Permitir tráfico del servicio a la base de datos
    db.connections.allowDefaultPortFrom(
      service,
      'Allow traffic from ECS service'
    );

    // Outputs
    new cdk.CfnOutput(this, 'LoadBalancerDNS', {
      value: alb.loadBalancerDnsName,
      description: 'DNS del Application Load Balancer',
      exportName: 'FleetALBDNS',
    });

    new cdk.CfnOutput(this, 'DatabaseEndpoint', {
      value: db.clusterEndpoint.hostname,
      description: 'Endpoint de la base de datos Aurora',
      exportName: 'FleetDBEndpoint',
    });

    new cdk.CfnOutput(this, 'S3BucketName', {
      value: documentsBucket.bucketName,
      description: 'Nombre del bucket S3',
      exportName: 'FleetS3Bucket',
    });

    new cdk.CfnOutput(this, 'SQSQueueUrl', {
      value: SQS_queue.queueUrl,
      description: 'URL de la cola SQS',
      exportName: 'FleetSQSQueue',
    });
  }
}
