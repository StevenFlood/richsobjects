package com.github.ryanbrainard.richobjects.api.client;

import com.github.ryanbrainard.richsobjects.api.client.SfdcApiClient;
import com.github.ryanbrainard.richsobjects.api.model.BasicSObjectInformation;
import com.github.ryanbrainard.richsobjects.api.model.GlobalDescription;
import com.github.ryanbrainard.richsobjects.api.model.QueryResult;
import com.github.ryanbrainard.richsobjects.api.model.SObjectDescription;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * @author Ryan Brainard
 */
public class SfdcApiJerseyClient implements SfdcApiClient {

    private final WebResource baseResource;
    private final WebResource versionedDateResource;
    private final WebResource sobjectsResource;

    SfdcApiJerseyClient(String accessToken, String apiEndpoint, String version){
        final ClientConfig config = new DefaultClientConfig();
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        config.getClasses().add(ObjectMapperProvider.class);

        final Client jerseyClient = Client.create(config);
        jerseyClient.addFilter(new AuthorizationHeaderFilter(accessToken));
        baseResource = jerseyClient.resource(apiEndpoint);
        versionedDateResource = jerseyClient.resource(apiEndpoint + "/services/data/" + version);
        sobjectsResource = versionedDateResource.path("/sobjects");
    }

    @Override
    public GlobalDescription describeGlobal() {
        return sobjectsResource.get(GlobalDescription.class);
    }

    @Override
    public BasicSObjectInformation describeSObjectBasic(String type) {
        return sobjectsResource.path("/" + type).get(BasicSObjectInformation.class);
    }

    @Override
    public SObjectDescription describeSObject(String type) {
        return sobjectsResource.path("/" + type + "/describe").get(SObjectDescription.class);
    }

    @Override
    public String createSObject(String type, Map<String, ?> record) {
        return sobjectsResource.path("/" + type)
                .entity(record, MediaType.APPLICATION_JSON_TYPE)
                .post(Map.class).get("id").toString();
    }

    @Override
    public void updateSObject(String type, String id, Map<String, ?> record) {
        sobjectsResource.path("/" + type + "/" + id).queryParam("_HttpMethod", "PATCH")
                .entity(record, MediaType.APPLICATION_JSON_TYPE)
                .post();
    }

    @Override
    public void deleteSObject(String type, String id) {
        sobjectsResource.path("/" + type + "/" + id).delete();
    }

    @Override
    public Map<String, ?> getSObject(String type, String id) {
        //noinspection unchecked
        return (Map<String, ?>) sobjectsResource.path("/" + type + "/" + id).get(Map.class);
    }

    @Override
    public QueryResult query(String soql) {
        return versionedDateResource.path("/query").queryParam("q", soql).get(QueryResult.class);
    }

    @Override
    public QueryResult queryMore(String nextRecordsUrl) {
        return baseResource.path(nextRecordsUrl).get(QueryResult.class);
    }
}