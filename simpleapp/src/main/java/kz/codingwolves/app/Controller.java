package kz.codingwolves.app;

import kz.codingwolves.framework.Api;
import kz.codingwolves.framework.QueryParam;

/**
 * Created by sagynysh on 2/16/17.
 */
public class Controller {

    @Api(value = "/")
    public String index() {
        return "Hello world!";
    }

    @Api(value = "/concat")
    public String concat(@QueryParam(value = "a") String a, @QueryParam(value = "b") String b) {
        return a + b;
    }

    @Api(value = "/add")
    public String add(@QueryParam(value = "a") String a, @QueryParam(value = "b") String b) {
        return ((Integer) (Integer.valueOf(a) + Integer.valueOf(b))).toString();
    }
}
