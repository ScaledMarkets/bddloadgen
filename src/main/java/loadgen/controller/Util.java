package loadgen.controller;


public class Util
{
	static <T> boolean arrayContains(T[] ar, T value)
	{
		for (T elt : ar) if (elt.equals(value)) return true;
		return false;
	}
}

