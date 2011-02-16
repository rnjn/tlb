package tlb;

/**
 * @understands TBL constants
 */
public interface TlbConstants {
    static final String TYPE_OF_SERVER = "TYPE_OF_SERVER";

    public interface Go {
        static final String GO_SERVER_URL = "GO_SERVER_URL";
        static final String GO_PIPELINE_NAME = "GO_PIPELINE_NAME";
        static final String GO_STAGE_NAME = "GO_STAGE_NAME";
        static final String GO_JOB_NAME = "GO_JOB_NAME";
        static final String GO_STAGE_COUNTER = "GO_STAGE_COUNTER";
        static final String GO_PIPELINE_COUNTER = "GO_PIPELINE_COUNTER";
        static final String GO_PIPELINE_LABEL = "GO_PIPELINE_LABEL";
        static final String GO_STAGE_FEED_MAX_SEARCH_DEPTH = "GO_STAGE_FEED_MAX_SEARCH_DEPTH";
    }

    public interface TlbServer {
        static final String TLB_JOB_NAME = "TLB_JOB_NAME";
        static final String TLB_BASE_URL = "TLB_BASE_URL";
        static final String TLB_PARTITION_NUMBER = "TLB_PARTITION_NUMBER";
        static final String TLB_TOTAL_PARTITIONS = "TLB_TOTAL_PARTITIONS";
        static final String TLB_JOB_VERSION = "TLB_JOB_VERSION";
    }

    static final String PASSWORD = "TLB_PASSWORD";
    static final String USERNAME = "TLB_USERNAME";
    static final String TLB_SPLITTER = "TLB_SPLITTER";
    static final String TEST_SUBSET_SIZE_FILE = "tlb/subset_size";
    static final String TLB_PREFERRED_SPLITTERS = "TLB_PREFERRED_SPLITTERS";
    static final String TLB_TMP_DIR = "TLB_TMP_DIR";
    static final String TLB_ORDERER = "TLB_ORDERER";
    static final String TLB_SMOOTHING_FACTOR = "TLB_SMOOTHING_FACTOR";

    static final String TLB_APP = "TLB_APP";

    public interface Balancer {
        static final String TLB_BALANCER_PORT = "TLB_BALANCER_PORT";
        static final String QUERY = "query";
    }

    public interface Server {
        static final String REPO_FACTORY = "repo_factory";
        static final String REQUEST_NAMESPACE = "namespace";
        static final String TLB_SERVER_PORT = "TLB_SERVER_PORT";
        static final String TLB_DATA_DIR = "TLB_DATA_DIR";
        static final String DEFAULT_TLB_DATA_DIR = "tlb_store";
        static final String LISTING_VERSION = "listing_version";
        static final String TLB_VERSION_LIFE_IN_DAYS = "TLB_VERSION_LIFE_IN_DAYS";
    }
}
