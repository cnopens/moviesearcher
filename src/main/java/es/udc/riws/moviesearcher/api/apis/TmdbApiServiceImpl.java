package es.udc.riws.moviesearcher.api.apis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import es.udc.riws.moviesearcher.model.Movie;
import es.udc.riws.moviesearcher.model.Person;
import es.udc.riws.moviesearcher.model.Person.TypePerson;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbMovies;
import info.movito.themoviedbapi.model.Credits;
import info.movito.themoviedbapi.model.Genre;
import info.movito.themoviedbapi.model.MovieDb;
import info.movito.themoviedbapi.model.core.MovieResultsPage;
import info.movito.themoviedbapi.model.people.PersonCast;
import info.movito.themoviedbapi.model.people.PersonCrew;

/**
 * Librería: https://github.com/holgerbrandl/themoviedbapi/
 * 
 * @author Carlos
 *
 */
@Service
public class TmdbApiServiceImpl implements GenericApiService {

	public static final String API_KEY = "956651bac38135dfba7377945f6809a9";

	private static final int NUM_PAGINAS = 2;

	private static final int TIME_TO_SLEEP = 6000;

	private final AtomicLong counter = new AtomicLong();

	@Override
	public List<Movie> getMovies() {

		TmdbApi tmdbApi = new TmdbApi(API_KEY);

		TmdbMovies moviesTmdb = tmdbApi.getMovies();

		// Creamos una lista de películas
		List<Movie> movies = new ArrayList<Movie>();
		// Recuperamos las películas en español
		MovieResultsPage results = null;
		for (int i = 1; i <= NUM_PAGINAS; i++) {
			if (results == null) {
				results = moviesTmdb.getPopularMovieList("es", i);
			} else {
				results.getResults().addAll(moviesTmdb.getPopularMovieList("es", i).getResults());
			}
			// results.getResults().addAll(moviesTmdb.getTopRatedMovies("es",
			// i).getResults());
		}
		int processedMovies = 0;
		for (MovieDb result : results) {

			MovieDb movieDb = moviesTmdb.getMovie(result.getId(), "es");

			// Creamos una lista de géneros
			List<String> genres = new ArrayList<String>();
			if (movieDb.getGenres() != null) {
				for (Genre genre : movieDb.getGenres()) {
					genres.add(genre.getName());
				}
			}

			// Actores
			Credits credits = moviesTmdb.getCredits(result.getId());
			List<Person> people = new ArrayList<>();
			if (credits.getCast() != null) {
				for (PersonCast personCast : credits.getCast()) {
					people.add(new Person(personCast.getName(), personCast.getCharacter(), personCast.getOrder(),
							TypePerson.CAST));
				}
			}

			// Directores y escritores
			if (credits.getCrew() != null) {
				for (PersonCrew personCast : credits.getCrew()) {
					if (personCast.getJob().equals("Director")) {
						people.add(new Person(personCast.getName(), personCast.getDepartment(), null,
								TypePerson.DIRECTOR));
					} else if (personCast.getJob().equals("Screenplay")) {
						people.add(
								new Person(personCast.getName(), personCast.getDepartment(), null, TypePerson.WRITER));
					}
				}
			}

			String yearString = movieDb.getReleaseDate().split("-")[0];
			int year = Integer.valueOf(yearString);

			// Añadimos las películas recuperadas a la lista de películas
			Movie movie = new Movie(counter.incrementAndGet(), movieDb.getTitle(), movieDb.getOverview(),
					movieDb.getPosterPath(), movieDb.getVoteAverage(), year, movieDb.getRuntime(), people, genres);

			if (!movies.contains(movie)) {
				movies.add(movie);
			}

			processedMovies++;
			if (processedMovies % 10 == 0) {
				dormir();
			}
		}
		return movies;

	}

	private void dormir() {
		try {
			Thread.sleep(TIME_TO_SLEEP);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
