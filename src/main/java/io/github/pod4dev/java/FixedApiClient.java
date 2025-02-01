package io.github.pod4dev.java;

import io.github.pod4dev.libpodj.ApiClient;
import io.github.pod4dev.libpodj.ApiException;
import io.github.pod4dev.libpodj.JSON;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class FixedApiClient extends ApiClient {

    @Override
    public RequestBody serialize(Object obj, String contentType) throws ApiException {
        if (obj instanceof byte[]) {
            // Binary (byte array) body parameter support.
            return RequestBody.create((byte[]) obj, MediaType.parse(contentType));
        } else if (obj instanceof File) {
            // File body parameter support.
            return RequestBody.create((File) obj, MediaType.parse(contentType));
        } else if ("text/plain".equals(contentType) && obj instanceof String) {
            return RequestBody.create(((String) obj).getBytes(StandardCharsets.UTF_8), MediaType.parse(contentType));
        } else if (isJsonMime(contentType)) {
            String content;
            if (obj != null) {
                content = JSON.serialize(obj);
            } else {
                content = null;
            }
            return RequestBody.create(content.getBytes(StandardCharsets.UTF_8), MediaType.parse(contentType));
        } else if (obj instanceof String) {
            return RequestBody.create(((String) obj).getBytes(StandardCharsets.UTF_8), MediaType.parse(contentType));
        } else {
            throw new ApiException("Content type \"" + contentType + "\" is not supported");
        }
    }
}
