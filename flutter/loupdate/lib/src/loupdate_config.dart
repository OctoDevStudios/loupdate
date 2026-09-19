class LoupdateConfig {
  const LoupdateConfig({
    required this.endpoint,
    required this.region,
    required this.bucket,
    required this.accessKeyId,
    required this.secretAccessKey,
    required this.appId,
    this.forceOverride,
  });

  final String endpoint;
  final String region;
  final String bucket;
  final String accessKeyId;
  final String secretAccessKey;
  final String appId;
  final bool? forceOverride;

  Map<String, Object?> toMap() => {
        'endpoint': endpoint,
        'region': region,
        'bucket': bucket,
        'accessKeyId': accessKeyId,
        'secretAccessKey': secretAccessKey,
        'appId': appId,
        'forceOverride': forceOverride,
      };
}
