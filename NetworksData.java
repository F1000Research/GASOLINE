import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedList;

public class NetworksData 
{
    
	private HashMap<Integer,Integer> mapNet;
	private HashMap<String,Integer> mapNodi;
	private LinkedList<Grafo> gList;
	private int cont;
	private int i;
    
	public NetworksData() 
	{
		mapNet=new HashMap<Integer,Integer>();
		mapNodi=new HashMap<String,Integer>();
		gList=new LinkedList<Grafo>();
		cont=0;
		i=0;
	}
    
	public void newNetwork(String fname, String name) throws Exception
	{
        
		//Costruzione network
		int index=name.indexOf(".");
		Grafo g=new Grafo(name.substring(0,index));
		BufferedReader br=new BufferedReader(new FileReader(fname));
		//JOptionPane.showMessageDialog(adapter.getCySwingApplication().getJFrame(), task.getError());
		String str="";
		while((str=br.readLine())!=null)
		{
			String[] campi=str.split("\t");
			String idSource=campi[0].trim();
			String idDest=campi[1].trim();
			double weight=Double.parseDouble(campi[2]);
			if(!mapNodi.containsKey(idSource))
			{
				g.addNode(cont,idSource);
				mapNet.put(cont,i);
				mapNodi.put(idSource,cont);
				cont++;
			}
			if(!mapNodi.containsKey(idDest))
			{
				g.addNode(cont,idDest);
				mapNet.put(cont,i);
				mapNodi.put(idDest, cont);
				cont++;
			}
			g.addArc(mapNodi.get(idSource), mapNodi.get(idDest), weight);
		}
            
		br.close();
		gList.add(g);
		i++; 
        
	}

	public LinkedList<Grafo> getGList() 
	{
		return gList;
	}

	public HashMap<Integer, Integer> getMapNet() 
	{
		return mapNet;
	}

	public HashMap<String, Integer> getMapNodi() 
	{
		return mapNodi;
	}

	public void deleteAll()
	{
		gList=new LinkedList<Grafo>();
		mapNodi=new HashMap<String,Integer>();
		mapNet=new HashMap<Integer,Integer>();
		cont=0;
		i=0;
	}
    
	public boolean isEmpty()
	{
		return (gList.isEmpty() && mapNodi.isEmpty() && mapNet.isEmpty() && cont==0 && i==0);
	}
}
