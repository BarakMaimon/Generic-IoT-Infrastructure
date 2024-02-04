import java.io.Serializable;

public interface Message<K, D extends Serializable> extends Serializable {
    K getKey();

    D getMessage();
}


class MessageImpl implements Message<String, String> {
    String key;
    String value;

    public MessageImpl(String key, String value) {
        this.key = key;
        this.value = value;
    }


    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public String getMessage() {
        return this.value;
    }
}

