import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class ImportNetTask extends AbstractTask
{

	private NetworksData netData;
	private String fname;
	private String name;
    
	public ImportNetTask(NetworksData netData, String fname, String name) 
	{
		this.netData = netData;
		this.fname = fname;
		this.name = name;
	}
    
	public void run(TaskMonitor taskMonitor) 
	{
		if(taskMonitor == null)
			throw new IllegalStateException("Task Monitor is not set.");
		else if(taskMonitor != null) 
		{
			taskMonitor.setProgress(-1);
			taskMonitor.setStatusMessage("Import");
		}
		try 
		{
			netData.newNetwork(fname,name);
		} 
		catch (Exception ex) 
		{
			System.out.println("Error importing new Network");
			netData.deleteAll();
			return;
		}
	}

	public void cancel() 
	{}

	public String getTitle() 
	{
		return "Import Network";
	}
}