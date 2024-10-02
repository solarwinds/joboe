#!/usr/local/bin/thrift --gen cpp:pure_enums --gen php

namespace cpp collector.thrift
namespace java com.appoptics.ext.thriftgenerated

enum ResultCode
{
  OK,
  TRY_LATER,
  INVALID_API_KEY,
  LIMIT_EXCEEDED,
  REDIRECT
}

struct ResultMessage
{
  1: ResultCode result,
  2: string arg,
  // warning specifies a user-facing warning message; agents attempt to squelch repeated warnings,
  // so care should be taken to ensure that warning messages are consistent across all RPCs.
  4: string warning
}

enum EncodingType {
  BSON = 1,
  PROTOBUF = 2
}

enum HostType {
  PERSISTENT = 0,
  AWS_LAMBDA = 1
}

# we need each tracelyzer/host to be uniquely identified when getting
# settings, so here's everything from the heartbeat message that's
# currently used to identify hosts.
struct HostID
{
  1: string hostname,
  2: list<string> OBSOLETE_ip_addresses,    // deprecated, do not use
  3: string OBSOLETE_uuid,                  // deprecated, do not use
  4: i32 pid,
  5: string ec2InstanceID,
  6: string ec2AvailabilityZone,
  7: string dockerContainerID,
  8: list<string> macAddresses,
  9: string herokuDynoID,
  10: string azAppServiceInstanceID,
  11: HostType hostType
}

enum OboeSettingType {
  DEFAULT_SAMPLE_RATE = 2,
  LAYER_SAMPLE_RATE = 3,
  LAYER_APP_SAMPLE_RATE = 4,
  LAYER_HTTPHOST_SAMPLE_RATE = 5,
  CONFIG_STRING = 6,
  CONFIG_INT = 7
}

struct OboeSetting {
  1: OboeSettingType type,
  2: string flags,
  3: i64 timestamp,
  4: i64 value,
  5: string layer,
  7: map<string, binary> arguments,
  8: i64 ttl
}

struct SettingsResult {
  1: ResultCode result,
  2: string arg,
  3: list<OboeSetting> settings,
  // warning specifies a user-facing warning message; see note on MessageResult.warning for details.
  4: string warning
}

service Collector {
  ResultMessage postEvents(1: string apiKey, 2: list<binary> messages, 3: EncodingType enc, 4: HostID identity);
  ResultMessage postMetrics(1: string apiKey, 2: list<binary> messages, 3: EncodingType enc, 4: HostID identity);
  ResultMessage postStatus(1: string apiKey, 2: list<binary> messages, 3: EncodingType enc, 4: HostID identity);
  ResultMessage ping(1: string apiKey);
  SettingsResult getSettings(1: string apiKey, 2: HostID identity, 3: string tracelyzerVersion);
}
