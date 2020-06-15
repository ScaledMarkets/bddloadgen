package loadgen.testrunner;


import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
//import java.util.jar.*;
//import java.util.function.*;
//import java.util.*;


/** Utilities for "user test" programs.
	See https://drive.google.com/open?id=1oy17eN_W4QuP-CTMeWAnzAJBB1eIX0Nzj5GcRHkTkhY&authuser=0
	*/
public class TestRunnerUtil
{
    // Call these methods to delineate the start and end of tests.


    /** Set the beginning of the "whole test". Calling this demarcates the beginning
    	of a "WholeTest" event - that is, the time from when a test begins until
    	when it ends. */
    public static void timelogBeginTest()
    {
        timelogBegin(WholeTest);
    }


    /** Set the end of the "whole test". Calling this demarcates the end
    	of a "WholeTest" event. */
    public static void timelogEndTest()
    {
        timelogEnd(WholeTest);
    }


    /** Log the begin time of a test activity (event) within a test. The event
    	is uniquely identified by the request type, the request id, and the activity's
    	name (aka, the 'event' name). */
    public static void timelogBegin(String name)
    {
        try { writeDetailedTimeLogEntry(name, "Begin"); }
        catch (Exception ex) { ex.printStackTrace(System.err); }
    }


    /** Log the end time of a test activity (event) that is uniquely identified by the
    	request type, the id, and the activity's name. Every begin time must have a
    	corresponding end time: thus, for every call to timelogBegin, there should
    	be a call to timelogEnd (this is a runtime requirement: not a static code requirement). */
    public static void timelogEnd(String name)
    {
        try { writeDetailedTimeLogEntry(name, "End"); }
        catch (Exception ex) { ex.printStackTrace(System.err); }
    }


    // Test context methods: call these methods to read test context variables.


    /** Each test invocation is provided a unique Id. This Id is returned by this method. */
    public static Long getTestId()
    {
    	return localTestId.get();
    }


    /** The RequestType for which this test invocation is being run. */
    public static String getRequestType()
    {
    	return localRequestType.get();
    }


    /** Time (ms) since start of the entire Performance or Functional TestRun. */
    public static Long getTimeOffset()
    {
    	return localTimeOffset.get();
    }


    /** Only applies if the test is a performance test. Otherwise, returns null.
    	If a PerformanceRun, return the current request rate, at the time that
    	this method is called. */
    public static Double getRequestRate()
    {
    	return localRequestRate.get();
    }



    /*	Implementation -----------------------------------------------------------
    	Tests should not need to call any methods below this point. */


    private static ThreadLocal<Long> localTestId = new InheritableThreadLocal<Long>();
    private static ThreadLocal<String> localRequestType = new InheritableThreadLocal<String>();
    private static ThreadLocal<Long> localTimeOffset = new InheritableThreadLocal<Long>();
    private static ThreadLocal<Double> localRequestRate = new InheritableThreadLocal<Double>();

    private static String thisResultsDir = System.getenv("RESULTS_DIR");
    private static String thisDetailtimelogfileName = resultsDir() + "/detailtimelog.csv";
    private static String thisLockfile = resultsDir() + "/detaillockfile";


    /** Called by TestRunner - tests do not need to call this. */
    public static void setUniqueId(long id)
    {
    	localTestId.set(new Long(id));
    }


    /** Called by TestRunner - tests do not need to call this. */
    public static void setRequestType(String reqType)
    {
    	localRequestType.set(reqType);
    }


    /** Called by TestRunner - tests do not need to call this. */
    public static void setTimeOffset(long timeOffset)
    {
    	localTimeOffset.set(new Long(timeOffset));
    }


    /** Called by TestRunner - tests do not need to call this. */
    public static void setRequestRate(double reqRate)
    {
    	localRequestRate.set(new Double(reqRate));
    }


    /** Called by TestRunner - tests do not need to call this.
    	Remove any log file from the results directory, which is specified by
    	the environment variable 'RESULTS_DIR'. This method should be called at
    	the start of a test run. */
    public static void removeOldLogfile()
    {
    	File f = new File(thisDetailtimelogfileName);
        if (f.exists()) f.delete();
    }


    static void writeDetailedTimeLogEntry(String name, String beginOrEnd) throws IOException
    {
        if (name.equals("EndToEnd")) throw new RuntimeException("The name 'EndToEnd' is reserved");
        String reqType = getRequestType();
        Long id = getTestId();
        Double reqRate = getRequestRate();
        Long timeOffset = getTimeOffset();
        long timeMs = System.currentTimeMillis() - timeOffset.longValue();
        int timeout = 10;  // flock will timeout after this number of seconds.
        String command = "echo " + reqType + ", " + id + ", " + name + ", " + reqRate +
        	", " + beginOrEnd + ", " + timeMs + "ms >> " + thisDetailtimelogfileName;
        try { Runtime.getRuntime().exec(
        	"flock -w " + timeout + " " + thisLockfile + " -c \"" + command + "\"").waitFor(); }
        catch (InterruptedException ex) { ex.printStackTrace(System.err); }

        // debug
        // puts "Wrote detail entry, reqType=#{reqType}, ID=#{id}, name=#{name}, reqRate=#{reqRate}, beginOrEnd=#{beginOrEnd}, time=#{time.to_f}"
        // end debug
    }


    static String detailtimelogfileName()
    {
       return thisDetailtimelogfileName;
    }


    static String lockfile()
    {
        return thisLockfile;
    }


    static String resultsDir()
    {
        return thisResultsDir;
    }
}
