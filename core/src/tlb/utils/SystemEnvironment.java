package tlb.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tlb.TlbConstants.TLB_TMP_DIR;

/**
 * @understands reading the environment variables of the system on which tlb runs
 */
public class SystemEnvironment {
    private static final Pattern REF = Pattern.compile(".*\\$\\{(.+?)\\}.*");
    public static final String TMP_DIR = "java.io.tmpdir";

    private Map<String, String> variables;

    public static final Logger logger = Logger.getLogger(SystemEnvironment.class.getName());

    public static class EnvVar {
        public final String key;

        public EnvVar(String key) {
            this.key = key;
        }

        public String getValue(SystemEnvironment env) {
            return env.loadVal(this);
        }
    }

    public static class DefaultedEnvVar extends EnvVar {
        private final String defaultValue;

        public DefaultedEnvVar(String key, String defaultValue) {
            super(key);
            this.defaultValue = defaultValue;
        }

        @Override
        public String getValue(SystemEnvironment env) {
            String value = super.getValue(env);
            return value == null ? defaultValue : value;
        }
    }

    public SystemEnvironment(Map<String, String> variables) {
        this.variables = new HashMap<String, String>();
        for (Map.Entry<String, String> ent : variables.entrySet()) {
            this.variables.put(ent.getKey(), ent.getValue());
        }
    }

    public SystemEnvironment() {
        this(System.getenv());
    }

    private String loadVal(EnvVar var) {
        String[] keysInOrderOfPreference = var.key.split(":");
        for (String prefferedKey : keysInOrderOfPreference) {
            String value = variables.get(prefferedKey);
            if (value != null) {
                value = substituteRefs(value);
                return value;
            }
        }
        return null;
    }

    private String substituteRefs(String value) {
        if (value == null) return null;
        final Matcher matcher = REF.matcher(value);
        if (matcher.find()) {
            final String ref = matcher.group(1);
            return substituteRefs(value.replace(String.format("${%s}", ref), val(new EnvVar(ref))));
        }
        return value;
    }

    public String val(EnvVar envVar) {
        return envVar.getValue(this);
    }

    public String getDigest() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectOut = new ObjectOutputStream(out);
            objectOut.writeObject(variables);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return DigestUtils.md5Hex(out.toByteArray());
    }

    String tmpDir() {
        String tmpParent = val(new EnvVar(TLB_TMP_DIR));
        if (tmpParent == null) {
            tmpParent = System.getProperty(SystemEnvironment.TMP_DIR);
            logger.warn(String.format("defaulting tlb tmp directory to %s", tmpParent));
        }
        logger.info(String.format("using %s as tlb temp directory", tmpParent));
        return new File(tmpParent, getDigest()).getAbsolutePath();
    }
}
