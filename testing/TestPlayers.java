package testing;

import java.io.*;
import java.lang.reflect.Field;
import java.util.regex.*;

import org.apache.commons.io.*;
import org.apache.tools.ant.*;

/*
 * This class is used to automate testing of a BattleCode-player versus another player (future work might include multiple opponents).
 */
public class TestPlayers {

	//Define our current executing environment
	private static final String executingLocation = TestPlayers.class.getProtectionDomain().getCodeSource().getLocation().getPath().replace("bin/", "");
	//Define player to test
	private static final String playerA = "simplePastr";
	private static final String playerB = "examplefuncsplayer";
	//Define a list of maps to test on
	private static final String[] maps = new String[] { "backdoor", "bakedpotato", "castles", "divide", "neighbors", "onramp", "reticle", "rushlane", "stitch" };
	//Define the testing configuration for the BattleCode program
	private static final String testConfiguration = "bc.server.throttle=yield\nbc.server.throttle-count=2500\nbc.engine.debug-methods=false\nbc.engine.silence-a=false\nbc.engine.silence-b=false\nbc.engine.gc=false\nbc.engine.gc-rounds=50\nbc.engine.upkeep=true\nbc.engine.breakpoints=false\nbc.engine.bytecodes-used=true\nbc.client.opengl=false\nbc.client.use-models=true\nbc.client.renderprefs2d=\nbc.client.renderprefs3d=\nbc.client.sound-on=false\nbc.client.check-updates=true\nbc.client.viewer-delay=50\nbc.server.transcribe-input=matches\\\\match.rms\nbc.server.transcribe-output=matches\\\\transcribed.txt\n";
	//Define the filename for the default configurations file for the BattleCode program
	private static final String defaultConfigurationFilename = "bc.conf";
	//Define the filename for the backup of the default configuration file
	private static final String backupConfigurationFilename = "bc_backup.conf";
	//Define the name for the root folder to save the game files of the test games into
	private static final String testGamesRootFolder = "tests";
	//Define the regex pattern to use when searching for the winning team's name
	private static final String regexPattern = ".*java.*server(.*)\\([AB]\\).*wins.*";
	//Define the filename for the temporary file used to write the output of the BattleCode program to
	private static final String tempOutputFilename = "tempOutput.txt";
	//Define the filename of the Ant file that is used to run the BattleCode program
	private static final String antBuildFilename = "build.xml";
	
	public static void main(String[] args) {
		
		//Open the default 'bc.conf' file
		File confFile = new File(String.format("%s%s", executingLocation, defaultConfigurationFilename));
		//Create a backup file
		File confFile_backup = new File(String.format("%s%s", executingLocation, backupConfigurationFilename));
		//Make a copy of the 'bc.conf' file, since we'll overwrite it with our settings in a moment
		try {
			FileUtils.copyFile(confFile, confFile_backup);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Print some info
		System.out.println(String.format("Running test for player '%s' versus player '%s', on %d maps", playerA, playerB, maps.length));
		System.out.println("");
		
		//We will need to keep track of #wins for each player
		int playerAWins = 0;
		int playerBWins = 0;
		
		//Do this for all maps
		for (int i = 0; i < maps.length; i++) {
			try {
				//Get current map
				String currentMap = maps[i];
				//Create a file to save results to
				String saveFileName = String.format("%s%s/%s/%s_%s_%s.rms", executingLocation, testGamesRootFolder, playerA, playerA, playerB, currentMap);
				
				//Re-write the 'bc.conf' file
				String bcConfTest = String.format("%s", testConfiguration);
				bcConfTest = String.format("%s\nbc.game.maps=%s", bcConfTest, currentMap);
				bcConfTest = String.format("%s\nbc.game.team-a=%s", bcConfTest, playerA);
				bcConfTest = String.format("%s\nbc.game.team-b=%s", bcConfTest, playerB);
				bcConfTest = String.format("%s\nbc.server.save-file=%s", bcConfTest, saveFileName);
				//Write the newly created configuration string to the 'bc.conf' file
				PrintWriter writer = new PrintWriter(confFile);
				writer.write(bcConfTest);
				//Make sure to close the writer
				writer.close();
				
				//Read output
				String output = getBattleCodeResult();
				//Determine winner
				if (output.contains("wins")) {
					//Match a regex to the output
					Pattern winPattern = Pattern.compile(regexPattern);
					Matcher matcher = winPattern.matcher(output);
					if (matcher.find()) {
						//Read the winning team
						String winLine = matcher.group();
						if (winLine.contains(playerA)) {
							//Player A won
							playerAWins++;
							System.out.println(String.format("'%s' won on %s", playerA, maps[i]));
						}
						else if (winLine.contains(playerB)) {
							//Player B won
							playerBWins++;
							System.out.println(String.format("'%s' won on %s", playerB, maps[i]));
						}
					}
				}
				else {
					//Something has gone terribly wrong, possibly a draw
					System.out.println("Error or draw, or whatever");
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
				
		//Copy back the original 'bc.conf' file, and then delete the backup
		try {
			FileUtils.copyFile(confFile_backup, confFile);
			FileUtils.deleteQuietly(confFile_backup);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Print final results
		System.out.println("");
		System.out.println(String.format("'%s' (A) won %d matches", playerA, playerAWins));
		System.out.println(String.format("'%s' (B) won %d matches", playerB, playerBWins));
	}

	/*
	 * This method runs the BattleCode program via Ant, through the 'build.xml' file in the root folder.
	 */
	private static String getBattleCodeResult() {
		String returnValue = "";
		try {
			//We need a temporary file for the output that the BattleCode program sends to the console
			File tempOutputFile = new File(String.format("%s%s", executingLocation, tempOutputFilename));
			FileOutputStream tempOutputStream = new FileOutputStream(tempOutputFile);
			PrintStream tempPrintStream = new PrintStream(tempOutputStream);
			LoggedPrintStream tempLogger = LoggedPrintStream.create(tempPrintStream);
			
			//This is the Ant file that we are going to be running from
			File buildFile = new File(String.format("%s%s", executingLocation, antBuildFilename));
			//This next part is from: http://www.ibm.com/developerworks/websphere/library/techarticles/0502_gawor/0502_gawor.html#sec2b
			Project p = new Project();
			p.setUserProperty("ant.file", buildFile.getAbsolutePath());
			//We need a logger here to make sure the output has somewhere to go
			DefaultLogger consoleLogger = new DefaultLogger();
			consoleLogger.setErrorPrintStream(System.err);
			consoleLogger.setOutputPrintStream(tempLogger);
			consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
			//Add the logger to the project as a 
			p.addBuildListener(consoleLogger);
			
			try {
				//Tell our project that we started
				p.fireBuildStarted();
				//Initialise the project
				p.init();
				//Add a helper to the project to parse the Ant file
				ProjectHelper helper = ProjectHelper.getProjectHelper();
				p.addReference("ant.projectHelper", helper);
				helper.parse(p, buildFile);
				//We need to run the 'file' target of the Ant file, since that runs the match and writes it to a file
				p.executeTarget("file");
				//Tell our project we have finished
				p.fireBuildFinished(null);
			} catch (BuildException e) {
				//Make sure the project knows to close itself
				p.fireBuildFinished(e);
			}
			
			//Return the output
			returnValue = tempLogger.buf.toString();
			
			//Make sure the temporary output file isn't used anymore
			tempOutputStream.close();
			tempPrintStream.close();
			tempLogger.close();
			//Delete it
			FileUtils.deleteQuietly(tempOutputFile);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
				
		return returnValue;
	}
	
	/*
	 * This class was copied from:
	 *   http://stackoverflow.com/questions/4334808/how-could-i-read-java-console-output-into-a-string-buffer
	 * and is used to read the output generated by the BattleCode program.
	 */
	private static class LoggedPrintStream extends PrintStream {

	    final StringBuilder buf;
	    @SuppressWarnings("unused")
		final PrintStream underlying;

	    LoggedPrintStream(StringBuilder sb, OutputStream os, PrintStream ul) {
	        super(os);
	        this.buf = sb;
	        this.underlying = ul;
	    }

	    public static LoggedPrintStream create(PrintStream toLog) {
	        try {
	            final StringBuilder sb = new StringBuilder();
	            Field f = FilterOutputStream.class.getDeclaredField("out");
	            f.setAccessible(true);
	            OutputStream psout = (OutputStream) f.get(toLog);
	            return new LoggedPrintStream(sb, new FilterOutputStream(psout) {
	                public void write(int b) throws IOException {
	                    super.write(b);
	                    sb.append((char)b);
	                }
	            }, toLog);
	        } catch (NoSuchFieldException shouldNotHappen) {
	        } catch (IllegalArgumentException shouldNotHappen) {
	        } catch (IllegalAccessException shouldNotHappen) {
	        }
	        return null;
	    }
	}
}
