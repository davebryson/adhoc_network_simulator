package mobicomp.emu;

/*
 * Options
 *
 * this class saves the global settings of the emulator
 *
 */

public class Options
{
	// what output should the emulator produce
	public static boolean outputPacketSent = false;
	public static boolean outputPacketLost = false;
	public static boolean outputPacketOverflow = false;
	
	// output settings
	public static boolean printToConsole = false;
	public static boolean addNodePrefix = true;
	public static int charBufferSize = 25*80;
	
	// misc settings
	public static int packetBufferSize = 20;
	public static int flashTime = 800;
}