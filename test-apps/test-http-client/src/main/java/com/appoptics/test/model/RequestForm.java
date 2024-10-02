package com.appoptics.test.model;

public class RequestForm {
    private String url;
    private boolean async;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            url = "http://" + url;
        }
        this.url = url;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }
}
