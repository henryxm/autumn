package cn.org.autumn.config;

public enum InputType {
    StringType("string"),
    BooleanType("boolean"),
    NumberType("number"),
    IntegerType("integer"),
    LongType("long"),
    FloatType("float"),
    DoubleType("double"),
    DecimalType("decimal"),
    JsonType("json"),
    ArrayType("array"),
    SelectionType("selection"),
    ;
    String value;

    InputType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}