import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.service.util.CyServiceRegistrar;
import java.util.Properties;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.application.swing.CytoPanelName;

public class AlignmentTask extends AbstractTask 
{
	private CySwingAppAdapter adapter;
	private double sigma;
	private int iterExtend;
	private int iterSeed;
	private double overlap;
	private int refine;
	private int minComplexSize;
	private boolean pairScores;
	private String homologyFileName;
	private String destFolder;
	private NetworksData netData;
	private TaskMonitor taskMonitor = null;
	private OrderedList<Allineamento> rankAlign;
	private Vector<String> ontFiles;
	private AlignmentPanel aPane;
    
	public AlignmentTask(double sigma, int iterExtend, int iterSeed, double overlap, 
	int refine, int minComplexSize, boolean pairScores, String homFName, NetworksData netData, String destFolder, 
	AlignmentPanel aPane, Vector<String> ontFiles, CySwingAppAdapter adapter) 
	{
		this.sigma=sigma;
		this.iterExtend=iterExtend;
		this.iterSeed=iterSeed;
		this.overlap=overlap;
		this.refine=refine;
		this.minComplexSize=minComplexSize;
		this.pairScores=pairScores;
		this.homologyFileName=homFName;
		this.netData=netData;
		this.destFolder=destFolder;
		this.aPane=aPane;
		this.ontFiles=ontFiles;
		this.adapter=adapter;
	}   
    
	public void run(TaskMonitor taskMonitor) 
	{
		if (taskMonitor==null)
			throw new IllegalStateException("Task Monitor is not set.");
		else if(taskMonitor!=null) 
		{
			taskMonitor.setProgress(0.0);
			taskMonitor.setStatusMessage("Aligning");
		}
		try 
		{
			rankAlign=GASOLINE.startAlignment(sigma, iterExtend, iterSeed, overlap, refine, minComplexSize, pairScores, homologyFileName, netData, destFolder, taskMonitor);
			if(rankAlign!=null)
			{
				CytoPanel resPanel=adapter.getCySwingApplication().getCytoPanel(CytoPanelName.EAST);
				resPanel.setState(CytoPanelState.DOCK);
				ResultsPanel alignRes = new ResultsPanel(rankAlign, netData.getGList(), ontFiles, adapter);
				CyServiceRegistrar csr=adapter.getCyServiceRegistrar();
				csr.registerService(alignRes,CytoPanelComponent.class, new Properties());
			}
			aPane.enable();
		} 
		catch (Exception ex) 
		{
			ex.printStackTrace();
			JOptionPane.showMessageDialog(adapter.getCySwingApplication().getJFrame(), "Alignment Error");
			aPane.enable();
			return;
		}
	}
	
	public void cancel() 
	{}

	public String getTitle() 
	{
		return "Aligning networks...";
	}
    
}