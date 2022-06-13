package com.tracelytics.joboe;

import com.tracelytics.joboe.rpc.HostType;

import java.util.List;

/**
 * A combination of values to identify a host that makes the RPC call
 * @author pluk
 *
 */
public class HostId {
    private final String hostname;
    private final int pid;
    private final String ec2InstanceId;
    private final String ec2AvailabilityZone;
    private final String dockerContainerId;
    private final String herokuDynoId;
    private final String azureInstanceId;
    private List<String> macAddresses;
    private final HostType hostType;
    private final String uamsClientId;
    
    public HostId(String hostname, int pid, List<String> macAddresses, String ec2InstanceId, String ec2AvailabilityZone, String dockerContainerId, String herokuDynoId, String azureInstanceId, HostType hostType, String uamsClientId) {
        super();
        this.hostname = hostname;
        this.pid = pid;
        this.macAddresses = macAddresses;
        this.ec2InstanceId = ec2InstanceId;
        this.ec2AvailabilityZone = ec2AvailabilityZone;
        this.dockerContainerId = dockerContainerId;
        this.herokuDynoId = herokuDynoId;
        this.azureInstanceId = azureInstanceId;
        this.hostType = hostType;
        this.uamsClientId = uamsClientId;
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public int getPid() {
        return pid;
    }
    
    public String getEc2AvailabilityZone() {
        return ec2AvailabilityZone;
    }
    
    public String getEc2InstanceId() {
        return ec2InstanceId;
    }
    
    public String getDockerContainerId() {
        return dockerContainerId;
    }
    
    public String getHerokuDynoId() {
        return herokuDynoId;
    }

    public String getAzureInstanceId() {
        return azureInstanceId;
    }

    public List<String> getMacAddresses() {
        return macAddresses;
    }

    public HostType getHostType() {
        return hostType;
    }

    public String getUamsClientId() {
        return uamsClientId;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("HostId [");
        result.append("hostname=" + hostname);
        if (ec2InstanceId != null) {
            result.append(", ec2InstanceId=" + ec2InstanceId);
        }
        if (ec2AvailabilityZone != null) {
            result.append(", ec2AvailabilityZone=" + ec2AvailabilityZone);
        }
        if (dockerContainerId != null) {
            result.append(", dockerContainerId=" + dockerContainerId);
        }
        if (herokuDynoId != null) {
            result.append(", herokuDynoId=" + herokuDynoId);
        }
        if (azureInstanceId != null) {
            result.append(", azureInstanceId=" + azureInstanceId);
        }
        if (macAddresses != null) {
            result.append(", macAddresses=" + macAddresses);
        }
        if (hostType != null) {
            result.append(", hostType=" + hostType);
        }
        if (uamsClientId != null) {
            result.append(", uamsClientId=" + uamsClientId);
        }
        
        result.append("]");
        return result.toString();
    }
}
