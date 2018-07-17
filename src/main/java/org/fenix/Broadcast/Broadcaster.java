package org.fenix.Broadcast;

import org.fenix.llanfair.Run;
import org.fenix.llanfair.Run.State;
import org.fenix.llanfair.Segment;
import org.fenix.llanfair.Time;

import java.lang.*;

import java.beans.PropertyChangeEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class Broadcaster {

  private static final String LOG_PREFIX = "Broadcaster: ";
  private static final String BROADCAST_URL = "http://localhost:8080";

  private Run run;

  private static final ExecutorService executor = Executors.newSingleThreadExecutor();

  public Broadcaster() {
  }

  public Broadcaster(Run run) {
    this.run = run;
  }

  public void setRun(Run run) {
    System.out.println("New run");
    this.run = run;
  }

  public void processPropertyChangeEvent(PropertyChangeEvent event) {
    String property = event.getPropertyName();
    JsonObject retObj = null;
		if (Run.STATE_PROPERTY.equals(property)) {
      State runState = run.getState();
			if (runState.equals(State.ONGOING)) {
				System.out.println(LOG_PREFIX + "Run started");
			} else if (runState.equals(State.STOPPED)) {
				System.out.println(LOG_PREFIX + "Run stopped");
			} else if (runState.equals(State.READY)) {
        System.out.println(LOG_PREFIX + "Run reset");
      }
		} else if (Run.CURRENT_SEGMENT_PROPERTY.equals(property)) {
      System.out.println(LOG_PREFIX + "New segment");
      retObj = fireSegmentEvent(event);
		} else if (Run.DELAYED_START_PROPERTY.equals(property)) {

		}

    if (null != retObj) {
      final String stringEntity = retObj.toString();
      executor.submit(() -> sendBroadcast(stringEntity));
    }
  }

  private JsonObject fireSegmentEvent(PropertyChangeEvent event) {
    int prevSegment = (int) event.getOldValue();
    int currentSegment = (int) event.getNewValue();
    if (currentSegment > prevSegment && prevSegment > -1) {
		    Segment segment = run.getSegment(prevSegment);
        Time liveRun = segment.getTime(Segment.LIVE);
        Time bestRun = segment.getTime(Segment.BEST);

        JsonObjectBuilder bld = Json.createObjectBuilder();
        bld.add("type", "SEGMENT").add("name", run.getName()).add("segment", segment.getName()).add("liveTime", liveRun.getMilliseconds()).add("bestTime", bestRun.getMilliseconds());
        return bld.build();
    }

    return null;

  }

  private void sendBroadcast(String stringEntity) {
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpPost request = new HttpPost(BROADCAST_URL);
      request.setHeader("Content-Type", "application/json");
      request.setEntity(new StringEntity(stringEntity));
      HttpResponse response = client.execute(request);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
