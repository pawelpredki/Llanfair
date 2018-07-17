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
  private static final String BROADCAST_URL = "http://localhost:8080/llanfair";

  private Run run;

  private static final ExecutorService executor = Executors.newSingleThreadExecutor();

  public Broadcaster() {
  }

  public Broadcaster(Run run) {
    this.run = run;
  }

  public void setRun(Run run) {
    this.run = run;
  }

  public void processPropertyChangeEvent(PropertyChangeEvent event) {
    String property = event.getPropertyName();
    JsonObject retObj = null;
		if (Run.STATE_PROPERTY.equals(property)) {
      retObj = fireStateEvent(event);
		} else if (Run.CURRENT_SEGMENT_PROPERTY.equals(property)) {
      retObj = fireSegmentEvent(event);
		} else if (Run.COMPLETED_ATTEMPT_COUNTER_PROPERTY.equals(property)) {
      retObj = fireCompletedRunEvent(event);
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
        Time liveTime = segment.getTime(Segment.LIVE);
        Time bestTime = segment.getTime(Segment.BEST);
        Time runTime = segment.getTime(Segment.RUN);
        Time bestDelta = segment.getTime(Segment.DELTA_BEST);
        Time runDelta = segment.getTime(Segment.DELTA_RUN);

        JsonObjectBuilder bld = Json.createObjectBuilder();
        bld.add("type", "SEGMENT")
        .add("name", run.getName())
        .add("segment", segment.getName())
        .add("segmentNo", currentSegment)
        .add("segmentTotal", run.getRowCount())
        .add("liveTime", liveTime.getMilliseconds())
        .add("bestTime", null != bestTime ? bestTime.getMilliseconds() : 0)
        .add("runTime", null != runTime ? runTime.getMilliseconds() : 0)
        .add("bestDelta", null != bestDelta ? bestDelta.getMilliseconds() : 0)
        .add("runDelta", null != runDelta ? runDelta.getMilliseconds() : 0);
        return bld.build();
    }
    return null;
  }

  private JsonObject fireStateEvent(PropertyChangeEvent event) {
    State runState = run.getState();
    if (runState.equals(State.ONGOING)) {
      return Json.createObjectBuilder().add("type", "RUN_START")
      .add("name", run.getName())
      .add("attempt", run.getNumberOfAttempts()).build();
    }
    return null;
  }

  private JsonObject fireCompletedRunEvent(PropertyChangeEvent event) {
    Time liveTime = run.getTime(Segment.LIVE);
    Time runTime = run.getTime(Segment.RUN);

    return Json.createObjectBuilder()
    .add("type", "RUN_COMPLETED")
    .add("name", run.getName())
    .add("attempt", run.getNumberOfAttempts())
    .add("completed", run.getNumberOfCompletedAttempts())
    .add("liveTime", null != liveTime ? liveTime.getMilliseconds() : 0)
    .add("runTime", null != runTime ? runTime.getMilliseconds() : 0).build();
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
