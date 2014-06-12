import java.io.*;
import java.util.zip.DataFormatException;
import javax.swing.*;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class FileFormatTask extends AbstractTask
{
	private TaskMonitor taskMonitor = null;
	private File inputFile;
	private String header;
	private String regex;
	private String errorMess;

	public FileFormatTask(File inputFile, String header, String regex) 
	{
		this.inputFile = inputFile;
		this.header = header;
		this.regex = regex;
		errorMess=null;
	}
    
	public void run(TaskMonitor taskMonitor)
	{
		if(taskMonitor==null)
			throw new IllegalStateException("Task Monitor is not set.");
		else if(taskMonitor!=null)
		{
			taskMonitor.setProgress(-1);
			taskMonitor.setStatusMessage("Verifying file format...");
		}
		int line=0;
		try 
		{
			BufferedReader br=new BufferedReader(new FileReader(inputFile));
			String str="";
			//Header check
			if(header!=null)
			{
				line++;
				str=br.readLine();
				if(!str.equals(header))
					throw new DataFormatException();
			}
			//File data check
			while((str=br.readLine())!=null)
			{
				line++;
				if(!str.matches(regex))
					throw new DataFormatException();
			}
			br.close();
		}
		catch(FileNotFoundException e)
		{
			errorMess="Error! File \""+inputFile.getName()+"\" not found";
			return;
		}
		catch(IOException e)
		{
			errorMess="Error reading \""+inputFile.getName()+"\"";
			return;
		}
		catch(DataFormatException e) 
		{
			errorMess="File \""+inputFile.getName()+"\" is not in the correct format. Error at line "+line+"!";
			return;
		}
	}

	public void cancel() 
	{}

	public String getTitle() 
	{
		return "Verifying file format...";
	}
	
	public String getError()
	{
		return errorMess;
	}
	
}
