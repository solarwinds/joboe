package controllers;

import play.libs.F;
import play.mvc.Result;
import play.mvc.Results;
import views.html.index;

public class FutureMapper {
	public static <S> F.Function<S, Result> getMapper() {
		return new F.Function<S, Result>() {
            public Result apply(S i) {
                return Results.ok(index.render("The answer is: " + i));
            }
        };
	}
}
