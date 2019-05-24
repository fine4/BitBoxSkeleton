package unimelb.bitbox;

import org.kohsuke.args4j.Option;
import unimelb.bitbox.util.HostPort;


public class CommandLineArgs {
    @Option(required = true, name = "-c", usage = "command")
    private String command;

    @Option(required = true, name = "-s", usage = "hostport")
    private String hostport;

    @Option(required = false, name = "-p", usage = "peer")
    private String peer;

    public String getCommand(){
        return command;
    }

    public String getServerHostport(){
        return hostport;
    }

    public String getPeer(){
        return peer;
    }
}
