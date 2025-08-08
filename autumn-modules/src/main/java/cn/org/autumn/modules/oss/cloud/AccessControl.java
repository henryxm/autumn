package cn.org.autumn.modules.oss.cloud;

public enum AccessControl {
    Default("default"),
    Private("private"),
    PublicRead("public-read"),
    PublicReadWrite("public-read-write");

    private final String acl;

    private AccessControl(String acl) {
        this.acl = acl;
    }

    public String toString() {
        return this.acl;
    }

    public static AccessControl parse(String acl) {
        AccessControl[] arr$ = values();
        int len$ = arr$.length;
        for (int i$ = 0; i$ < len$; ++i$) {
            AccessControl cacl = arr$[i$];
            if (cacl.toString().equals(acl)) {
                return cacl;
            }
        }
        throw new IllegalArgumentException("Unable to parse the provided acl " + acl);
    }
}
