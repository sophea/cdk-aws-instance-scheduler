package com.jst.lambda.model;

import java.util.List;

/**
 * Author: Mak Sophea
 * Date: 05/12/2021
 */
public class Data {
    private String name;
    private String account;
    private String region;
    private String utcOffset;
    private List<Tag> tags;
    private String hookUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getUtcOffset() {
        return utcOffset;
    }

    public void setUtcOffset(String utcOffset) {
        this.utcOffset = utcOffset;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public String getHookUrl() {
        return hookUrl;
    }

    public void setHookUrl(String hookUrl) {
        this.hookUrl = hookUrl;
    }
}
