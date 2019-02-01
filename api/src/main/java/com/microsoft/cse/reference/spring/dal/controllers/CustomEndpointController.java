package com.microsoft.cse.reference.spring.dal.controllers;

import com.google.common.collect.ImmutableMap;
import com.microsoft.cse.reference.spring.dal.converters.EmptyStringToNull;
import com.microsoft.cse.reference.spring.dal.converters.IntegerToBoolean;
import com.microsoft.cse.reference.spring.dal.models.PrincipalWithName;
import com.microsoft.cse.reference.spring.dal.models.Title;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.util.Arrays.asList;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;

/**
 *
 * Create a custom controller so that when a user hits a URL that's not formatted like the search endpoint
 *
 */
@RestController
public class CustomEndpointController {

    private IntegerToBoolean integerToBoolean = new IntegerToBoolean();
    private EmptyStringToNull emptyStringToNull = new EmptyStringToNull();
    private MongoTemplate mongoTemplate;
    private Tracer tracer;

    @Autowired
    public CustomEndpointController(MongoTemplate mongoTemplate, Tracer tracer) {
        this.mongoTemplate = mongoTemplate;
        this.tracer = tracer;
    }

    /**
     * Query the Principals collection via aggregation.
     *
     * Since the Mongo/Spring doesn't do a great job with the relational IMDb data we have
     *   and the SDK doesn't have a solution to access the data we want, we have to do some
     *   data transforming.
     *
     *   example Mongo query that gets us close:
     *   db.principals_mapping.aggregate([
     *      {$match: {"nconst":"nm0000428"}},
     *      {$lookup: {from:"titles", localField:"tconst", foreignField:"tconst", as: "title_info"}},
     *      {$project: {"title_info":{"$arrayElemAt": ["$title_info",0]}, "_id":0}}
     *      ])
     *
     * Unfortunately, it seems you can't suppress a field in a projection in Spring, so we get back
     *   data that doesn't properly map into a Title object like we'd want.
     *
     * Ideally: if this data was formatted in a more "MongoDB" way, then there would likely be
     *   a Title object embedded into the requested Principal object so no cross-collection,
     *   JOIN-like behavior would be needed. However, the data is not ideal in this way, so we have
     *   to come up with a way to return the needed information.
     *
     * @param nconst
     * @return List of Titles
     */
    @RequestMapping(method = RequestMethod.GET, value = "/people/{nconst}/titles")
    public List<Title> getAllTitles(@PathVariable String nconst) {
        final Span span = tracer.buildSpan("get-titles-from-person").start();
        span.log(ImmutableMap.of("event", "get-titles-from-person", "value", nconst));

        MatchOperation filterByNconst = match(Criteria.where("nconst").is(nconst));

        LookupOperation titleLookup = LookupOperation.newLookup()
                .from("titles")
                .localField("tconst")
                .foreignField("tconst")
                .as("title_info");

        Aggregation aggregation = Aggregation.newAggregation(
                filterByNconst,
                titleLookup,
                project("title_info")
        );

        AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(aggregation, "principals_mapping", Document.class);

        span.finish();
        return documentToTitleList(aggregationResults);
    }


    /**
     * Generates a list from the Principal data set based on the tconst
     *   and has a Person-like object instead of the id.
     *
     * @param tconst
     * @return List of PrincipalWithNames
     */
    @RequestMapping(method = RequestMethod.GET, value = "/titles/{tconst}/people")
    public List<PrincipalWithName> getAllPeople(@PathVariable String tconst) {
        final Span span = tracer.buildSpan("get-people-from-title").start();
        span.log(ImmutableMap.of("event", "query-people-from-title", "value", tconst));

        MatchOperation filterByNconst = match(Criteria.where("tconst").is(tconst));
        LookupOperation nameLookup = LookupOperation.newLookup()
                .from("names")
                .localField("nconst")
                .foreignField("nconst")
                .as("person");

        Aggregation aggregation = Aggregation.newAggregation(
                filterByNconst,
                nameLookup
        );

        AggregationResults<PrincipalWithName> results = mongoTemplate.aggregate(aggregation, "principals_mapping", PrincipalWithName.class);

        // removes a clunky "_id" field that is generated when searching MongoDb
        List<PrincipalWithName> mappedResults = results.getMappedResults();
        for (PrincipalWithName p: mappedResults) {
            p.person.remove("_id");
        }

        span.finish();
        return mappedResults;
    }


    /**
     * Generates a list from the Principal data set based on the tconst
     *   and has a Person-like object instead of the id.
     *
     * @param tconst
     * @return List of PrincipalWithNames
     */
    @RequestMapping(method = RequestMethod.GET, value = "/titles/{tconst}/crew")
    public List<PrincipalWithName> getAllCrew(@PathVariable String tconst) {
        final Span span = tracer.buildSpan("get-crew-from-title").start();
        span.log(ImmutableMap.of("event", "query-crew-from-title", "value", tconst));

        MatchOperation filterByNconst = match(Criteria.where("tconst").is(tconst));
        MatchOperation excludeCast = match(Criteria.where("category").ne("actress").andOperator(Criteria.where("category").ne("actor")));
        LookupOperation nameLookup = LookupOperation.newLookup()
                .from("names")
                .localField("nconst")
                .foreignField("nconst")
                .as("person");

        Aggregation aggregation = Aggregation.newAggregation(
                filterByNconst,
                excludeCast,
                nameLookup
        );

        AggregationResults<PrincipalWithName> results = mongoTemplate.aggregate(aggregation, "principals_mapping", PrincipalWithName.class);

        // removes a clunky "_id" field that is generated when searching MongoDb
        List<PrincipalWithName> mappedResults = results.getMappedResults();
        for (PrincipalWithName p: mappedResults) {
            p.person.remove("_id");
        }

        span.finish();
        return mappedResults;
    }


    /**
     * Generates a list from the Principal data set based on the tconst
     *   and has a Person-like object instead of the id.
     *
     * @param tconst
     * @return List of PrincipalWithNames
     */
    @RequestMapping(method = RequestMethod.GET, value = "/titles/{tconst}/cast")
    public List<PrincipalWithName> getAllCast(@PathVariable String tconst) {
        final Span span = tracer.buildSpan("get-cast-from-title").start();
        span.log(ImmutableMap.of("event", "query-cast-from-title", "value", tconst));

        MatchOperation filterByNconst = match(Criteria.where("tconst").is(tconst));
        MatchOperation includeCast = match(new Criteria().orOperator(Criteria.where("category").is("actor"), Criteria.where("category").is("actress")));
        LookupOperation nameLookup = LookupOperation.newLookup()
                .from("names")
                .localField("nconst")
                .foreignField("nconst")
                .as("person");

        Aggregation aggregation = Aggregation.newAggregation(
                filterByNconst,
                includeCast,
                nameLookup
        );

        AggregationResults<PrincipalWithName> results = mongoTemplate.aggregate(aggregation, "principals_mapping", PrincipalWithName.class);

        // removes a clunky "_id" field that is generated when searching MongoDb
        List<PrincipalWithName> mappedResults = results.getMappedResults();
        for (PrincipalWithName p: mappedResults) {
            p.person.remove("_id");
        }

        span.finish();
        return mappedResults;
    }


    /**
     * Reusable block of code to change the MongoDB document into being
     *   a Title for easier code reading
     *
     * @param input
     * @return List of Title
     */
    private List<Title> documentToTitleList(AggregationResults<Document> input) {
        List<Title> titleList = new ArrayList<>();
        Iterator<Document> iter = input.iterator();

        // Declares reused variables
        Title t;
        Document nextDoc, resultDoc;
        ArrayList<Document> titleInfo;

        while (iter.hasNext()){
            nextDoc = iter.next();
            titleInfo = (ArrayList<Document>) nextDoc.get("title_info");
            resultDoc = !titleInfo.isEmpty() ? titleInfo.get(0) : null;
            if (resultDoc != null) {
                t = new Title();

                t.tconst = (String) resultDoc.get("tconst");
                t.titleType = (String) resultDoc.get("titleType");
                t.primaryTitle = (String) resultDoc.get("primaryTitle");
                t.originalTitle = (String) resultDoc.get("originalTitle");
                t.isAdult = integerToBoolean.convert(((Integer) resultDoc.get("isAdult")));
                t.startYear = (Integer) resultDoc.get("startYear");
                String endYear = emptyStringToNull.convert((String) resultDoc.get("endYear"));
                t.endYear = endYear == null ? null : Integer.parseInt(endYear);
                t.runtimeMinutes = (Integer) resultDoc.get("runtimeMinutes");
                String genres = (String) resultDoc.get("genres");
                t.genres = genres != null ? asList((genres).split(",")) : null;
                titleList.add(t);
            }
        }

        return titleList;
    }
}
