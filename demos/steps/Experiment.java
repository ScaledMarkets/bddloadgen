package steps;


import loadgen.TestRunnerUtil;
import java.net.*;


public class Experiment
{
	private String reqType;
	private String targetURL;
	private String result;

	@Given ("^that we have a headless browser available$")
	public void do_given()
	{
		TestRunnerUtil.timelogBeginTest();
		reqType = TestRunnerUtil.getRequestType();
		targetURL = System.getenv("TARGET_URL");
	}

	@When ("^a headless client requests$")
	public void do_when()
	{
		TestRunnerUtil.timelogBegin("When");

		System.out.println("Entered When; reqType=" + reqType + ", ID=" +
			System.getenv("ID") + "; time(ms)=" + System.currentTimeMillis());

		System.out.println("Getting " + targetURL);

		URL url = new URL(targetURL);
		URLConnection con = url.openConnection();
		result = con.
		System.out.println("Exiting cucumber When; reqType=" + reqType +
			", ID=" + System.getenv("ID") + "; time(ms)=" + System.currentTimeMillis());

		TestRunnerUtil.timelogEnd("When");
	}

	@Then ("^a successful response is returned$")
	public void do_then()
	{
		TestRunnerUtil.timelogBegin("Then");
		System.out.println("Entered cucumber Then; reqType=" + reqType +
			", ID=" + System.getenv("ID") + "; time(ms)=" + System.currentTimeMillis());
		System.out.println("result=" + result);
		System.out.println("Exiting JBehave Then; reqType=" + reqType +
			", ID=" + System.getenv("ID") + "; time(ms)=" + System.currentTimeMillis());
		TestRunnerUtil.timelogEnd("Then");
		TestRunnerUtil.timelogEndTest();
	}
}
