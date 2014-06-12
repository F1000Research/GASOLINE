import java.io.*;
import java.util.*;
import javax.swing.JOptionPane;
import org.cytoscape.work.TaskMonitor;

public class GASOLINE
{
	static double scoreLabel=0.0;
	static double scoreTopology=0.0;
	static double SIGMA=7;
	static int ITER_EXTEND=200;
	static int ITER_SEED=200;
	static double OVERLAP=0.5;
	static int REFINE=10;
	static int MIN_COMPLEX_SIZE=5;
	static boolean PAIR_SCORES;
	static String PATH;
        static String homologyFileName;
	static NetworksData netData;
	static TaskMonitor task;
        
        public static OrderedList<Allineamento> startAlignment(double sigma, 
	int iterExtend, int iterSeed, double overlap, int refine, int minComplexSize, 
	boolean pairSc, String homFName, NetworksData nets, String destFolder, TaskMonitor taskMon) throws Exception
	{
		SIGMA=sigma;
                ITER_EXTEND=iterExtend;
                ITER_SEED=iterSeed;
		PAIR_SCORES=pairSc;
                netData=nets;
                PATH=destFolder;
		task=taskMon;
                
                if(overlap!=0)
			OVERLAP=overlap;
                if(refine!=0)
			REFINE=refine;
                if(minComplexSize!=0)    
			MIN_COMPLEX_SIZE=minComplexSize;
                
                homologyFileName=homFName;
            
                Grafo[] g=new Grafo[netData.getGList().size()];
                for(int j=0; j<netData.getGList().size(); j++)
		{
			g[j]=netData.getGList().get(j);
                }
		OrderedList<Allineamento> rankAlign=new OrderedList<Allineamento>();
		int i=0, j=0, k=0;
		int indIter=0;
		double bestScore=0.0;
		double bestScoreLabel=0.0;
		double bestScoreTopology=0.0;
        
		//Costruzione network
		HashMap<Integer,Integer> mapNet=netData.getMapNet();
		HashMap<String,Integer> mapNodi=netData.getMapNodi();
        
		//Lettura degli score di ortologia
		HashSet<Integer> allOrto=null;
		HashMap mapHomology;
		if(PAIR_SCORES)
		{
			mapHomology=new HashMap<Integer,HashMap<Integer,Double>>();
			allOrto=buildHomologiesFromPairwiseScores(g.length,mapNet,mapHomology,mapNodi);
		}
		else
		{
			mapHomology=new HashMap<Integer,Vector<String>>();
			allOrto=buildHomologiesFromCOG(g.length,mapNet,mapHomology,mapNodi);
		}
                if(allOrto==null)
			return null;
		
		//Inizio algoritmo
		double inizio=System.currentTimeMillis();
		
		//Filtraggio nodi
		HashSet<Integer>[] nodiCluster=filterNodes(g,allOrto);
		
		int maxPercent=Integer.MAX_VALUE;
		for(i=0;i<nodiCluster.length;i++)
		{
			if(nodiCluster[i].size()<maxPercent)
				maxPercent=nodiCluster[i].size();
		}
		
		boolean search=true;
		while(search)
		{
			double currentPercent=Math.min(((double)(indIter+1))/maxPercent,1.0);
			task.setProgress(currentPercent);
			for(i=0;i<nodiCluster.length;i++)
			{
				if(nodiCluster[i].isEmpty())
				{
					//Uno dei set ridotti si è svuotato: l'algoritmo può terminare
					search=false;
					break;
				}
			}
			if(!search)
				continue;
            
			//FASE 1) Ricerca dei seed ottimali
			double alignScore=0.0;
			Vector<Integer>[] seed;
			if(PAIR_SCORES)
			{
				GibbsSamplerSeedPairScores gs=new GibbsSamplerSeedPairScores(ITER_SEED, nodiCluster);
				seed=gs.runGibbs(mapHomology);
				alignScore=gs.getBestScore();
			}
			else
			{
				GibbsSamplerSeedCOG gs=new GibbsSamplerSeedCOG(ITER_SEED, nodiCluster);
				seed=gs.runGibbs(mapHomology);
				alignScore=gs.getBestScore();
			}
			if(alignScore<g.length-1)
			{
				for(i=0;i<seed.length;i++)
				{
					for(j=0;j<seed[i].size();j++)
						nodiCluster[i].remove(seed[i].get(j));
				}
				indIter++;
				continue;
			}	
			HashSet<Integer>[] adiacSeed;
			try
			{
				adiacSeed=getAdiacSeed(g,seed);
			}
			catch(Exception e)
			{
				for(i=0;i<seed.length;i++)
				{
					for(j=0;j<seed[i].size();j++)
						nodiCluster[i].remove(seed[i].get(j));
				}
				indIter++;
				continue;
			}
			
			double scoreAlignment=0.0;
			boolean contin=true;
			double[][][] adiacMaps= new double[g.length][][];
			int[][] ids=new int[g.length][];
			buildMaps(g, seed, adiacSeed, ids, adiacMaps);
			for(j=0;j<ids.length;j++)
			{
				if(ids[j].length==0)
					contin=false;
			}
			if(contin)
			{
				//FASE 2: estensione dell'allineamento di un passo (allineamento ottimale di archi)
				int[] align;
				if(PAIR_SCORES)
				{
					GibbsSamplerExtendPairScores gse=new GibbsSamplerExtendPairScores(ITER_EXTEND,ids,adiacMaps);
					align=gse.runGibbs(mapHomology);
					alignScore=gse.getBestScoreLabel();
				}
				else
				{
					GibbsSamplerExtendCOG gse=new GibbsSamplerExtendCOG(ITER_EXTEND,ids,adiacMaps);
					align=gse.runGibbs(mapHomology);
					alignScore=gse.getBestScoreLabel();
				}
				if(alignScore==g.length)
				{
					for(j=0;j<align.length;j++)
						seed[j].add(align[j]);
					scoreAlignment=scoreAlign(g,seed);
					for(j=0;j<adiacSeed.length;j++)
					{
						HashMap<Integer,Double> adiac=g[j].nodi().get(seed[j].get(seed[j].size()-1)).getAdiacs();
						Set<Integer> keys=adiac.keySet();
						Iterator<Integer> it=keys.iterator();
						while(it.hasNext())
							adiacSeed[j].add(it.next());
						for(k=0;k<seed[j].size();k++)
							adiacSeed[j].remove(seed[j].get(k));
					}
				}
			}
			
			//FASE 3) Eliminazione dei nodi dal set ridotto
			for(i=0;i<seed.length;i++)
			{
				for(j=0;j<seed[i].size();j++)
					nodiCluster[i].remove(seed[i].get(j));
			}
			
			//FASE 4) Estensione e raffinamento dei seed
			if(seed[0].size()>=2)
			{
				int maxSize=seed[0].size();
				Vector<Integer>[] copySeed=new Vector[g.length];
				for(i=0;i<seed.length;i++)
				{
					copySeed[i]=new Vector<Integer>();
					for(j=0;j<seed[i].size();j++)
						copySeed[i].add(seed[i].get(j));
				}
				Allineamento a=new Allineamento(copySeed, copySeed[0].size(), scoreAlignment);
				for(i=0;i<REFINE;i++)
				{
					if(seed[0].size()>2)
						removeNode(g,seed);
					double avgDens=modularity(g,seed);
					extendSeeds(g,seed,adiacSeed,mapHomology,avgDens);
					scoreAlignment=scoreAlign(g,seed);
					if(scoreAlignment*seed[0].size()>a.getIsc()*a.getComplexSize())
					{
						copySeed=new Vector[g.length];
						for(j=0;j<seed.length;j++)
						{
							copySeed[j]=new Vector<Integer>();
							for(k=0;k<seed[j].size();k++)
								copySeed[j].add(seed[j].get(k));
						}
						a=new Allineamento(copySeed, copySeed[0].size(), scoreAlignment);
					}
				}
                
				//Stampa del miglior allineamento locale trovato dopo la refine
				Vector<Integer>[] mapping=a.getMapping();
				if(mapping[0].size()>=MIN_COMPLEX_SIZE)
				{
					rankAlign.insertOrdered(a);
					/*System.out.println("COMPLEX SIZE: "+a.getComplexSize());
					System.out.println("ISC: "+a.getIsc());    
					System.out.println("ITERATION: "+(indIter+1)+"\n");*/
				}
			}
			indIter++;
			
		}
		double currentPercent=1.0;
		task.setProgress(currentPercent);
		
		//POST-PROCESSING: risoluzione overlapping
		checkOverlap(g.length, rankAlign);
        
		//Stampa allineamenti migliori
		writeAlignments(g, rankAlign);
		
		//Fine algoritmo
		double fine=System.currentTimeMillis();
		//System.out.println("Running time: "+(((float)(fine-inizio))/1000)+" sec \n");
                
                return rankAlign;
	}
    
	//Restituisce la lista dei nodi adiacenti ai seed dell'allineamento
	public static HashSet<Integer>[] getAdiacSeed(Grafo[] g, Vector<Integer>[] seed)
	{
		HashSet<Integer>[] adiacSeed=new HashSet[g.length];
		int i=0, j=0;
		for(i=0;i<adiacSeed.length;i++)
		{
			adiacSeed[i]=new HashSet<Integer>();
			for(j=0;j<seed[i].size();j++)
			{
				HashMap<Integer,Double> adiac=g[i].nodi().get(seed[i].get(j)).getAdiacs();
				Set<Integer> keys=adiac.keySet();
				Iterator<Integer> it=keys.iterator();
				while(it.hasNext())
					adiacSeed[i].add(it.next());
			}
			for(j=0;j<seed[i].size();j++)
				adiacSeed[i].remove(seed[i].get(j));
		}
		return adiacSeed;
	}
    
	//Costruisce le strutture dati necessarie per il GibbsSamplerExtend: 1) la lista degli id dei nodi adiacenti, 2) le mappe dei pesi
	//degli archi che collegano i nodi adiacenti ai nodi del seed
	public static void buildMaps(Grafo[] g, Vector<Integer>[] seed, HashSet<Integer>[] adiacSeed, int[][] ids, double[][][] adiacMaps)
	{
		int i=0, j=0, k=0;
		for(i=0;i<adiacSeed.length;i++)
		{
			ids[i]=new int[adiacSeed[i].size()];
			adiacMaps[i]=new double[adiacSeed[i].size()][seed[i].size()];
			Iterator<Integer> it=adiacSeed[i].iterator();
			j=0;
			while(it.hasNext())
			{
				int id=it.next();
				ids[i][j]=id;
				Nodo n=g[i].nodi().get(id);
				HashMap<Integer,Double> adiac=n.getAdiacs();
				for(k=0;k<seed[i].size();k++)
				{
					if(adiac.containsKey(seed[i].get(k)))
						adiacMaps[i][j][k]=adiac.get(seed[i].get(k));
					else
						adiacMaps[i][j][k]=0.0;
				}
				j++;
			}
		}
	}
    
	//Costruisce i sottografi dell'allineamento locale trovato
	public static Grafo buildSubgraph(Grafo gr, Vector<Integer> nodi)
	{
		int i=0, j=0;
		HashMap<Integer,Nodo> listaNodi=gr.nodi();
		Grafo subGr=new Grafo();
		for(i=0;i<nodi.size();i++)
			subGr.addNode(nodi.get(i),listaNodi.get(nodi.get(i)).getLabel());
		HashMap<Integer,Double> adiac=null;
		for(i=0;i<nodi.size();i++)
		{
			adiac=listaNodi.get(nodi.get(i)).getAdiacs();
			for(j=0;j<nodi.size();j++)
			{
				if(adiac.containsKey(nodi.get(j)))
					subGr.addArc(nodi.get(i),nodi.get(j),adiac.get(nodi.get(j)));
			}
		}
		return subGr;
	}
    
	//Calcola lo score di conservazione strutturale dell'allineamento trovato (circular sum)
	public static double scoreAlign(Grafo[] g, Vector<Integer>[] seed)
	{
		int i=0, j=0, k=0;
		double scoreTopology=0.0;
		double parzScoreDegree=0.0;
		double scorePair=0.0;
		double mapScore=0.0;
        
		for(i=0;i<seed.length-1;i++)
		{
			scorePair=0.0;
			for(j=0;j<seed[i].size();j++)
			{
				HashMap<Integer,Double> adiacSource=g[i].nodi().get(seed[i].get(j)).getAdiacs();
				double[] mapSource=new double[seed[i].size()];
				for(k=0;k<seed[i].size();k++)
				{
					if(adiacSource.containsKey(seed[i].get(k)))
						mapSource[k]=adiacSource.get(seed[i].get(k));
					else
						mapSource[k]=0.0;
				}
				HashMap<Integer,Double> adiacDest=g[i+1].nodi().get(seed[i+1].get(j)).getAdiacs();
				double[] mapDest=new double[seed[i+1].size()];
				for(k=0;k<seed[i+1].size();k++)
				{
					if(adiacDest.containsKey(seed[i+1].get(k)))
						mapDest[k]=adiacDest.get(seed[i+1].get(k));
					else
						mapDest[k]=0.0;
				}
				mapScore=0.0;
				for(k=0;k<mapSource.length;k++)
				{
					if((mapSource[k]!=0.0 && mapDest[k]!=0.0)||(mapSource[k]==0.0 && mapDest[k]==0.0))
						mapScore++;
				}
				mapScore=mapScore/mapSource.length;
				scorePair+=mapScore;
			}
			scoreTopology+=scorePair;
		}
		scorePair=0.0;
		for(j=0;j<seed[i].size();j++)
		{
			HashMap<Integer,Double> adiacSource=g[i].nodi().get(seed[i].get(j)).getAdiacs();
			double[] mapSource=new double[seed[i].size()];
			for(k=0;k<seed[i].size();k++)
			{
				if(adiacSource.containsKey(seed[i].get(k)))
					mapSource[k]=adiacSource.get(seed[i].get(k));
				else
					mapSource[k]=0.0;
			}
			HashMap<Integer,Double> adiacDest=g[0].nodi().get(seed[0].get(j)).getAdiacs();
			double[] mapDest=new double[seed[0].size()];
			for(k=0;k<seed[0].size();k++)
			{
				if(adiacDest.containsKey(seed[0].get(k)))
					mapDest[k]=adiacDest.get(seed[0].get(k));
				else
					mapDest[k]=0.0;
			}
			mapScore=0.0;
			for(k=0;k<mapSource.length;k++)
			{
				if((mapSource[k]!=0.0 && mapDest[k]!=0.0)||(mapSource[k]==0.0 && mapDest[k]==0.0))
					mapScore++;
			}
			mapScore=mapScore/mapSource.length;
			scorePair+=mapScore;
		}
		
		scoreTopology+=scorePair;
	        scoreTopology=scoreTopology/(seed[0].size()*g.length);
	        return scoreTopology;
	}
	
	public static double modularity(Grafo[] g, Vector<Integer>[] seed)
	{
		int i=0, j=0, k=0;
		double scoreAlign=1.0;
		for(i=0;i<seed.length;i++)
	        {
			double complexDegreeInt=0.0;
			double totalDegree=0.0;
			for(j=0;j<seed[i].size();j++)
			{
				HashMap<Integer,Double> adiacSource=g[i].nodi().get(seed[i].get(j)).getAdiacs();
				totalDegree+=g[i].nodi().get(seed[i].get(j)).grado();
				for(k=0;k<seed[i].size();k++)
				{
					if(adiacSource.containsKey(seed[i].get(k)))
						complexDegreeInt++;
				}
			}
			scoreAlign*=complexDegreeInt/totalDegree;
		}
		scoreAlign=scoreAlign/seed.length;
	        return scoreAlign;
	}
	
	//Eliminazione dai grafi dei nodi che non hanno ortologhi in tutte le specie e costruzione del set iniziale dei nodi per la ricerca
	//di allineamenti di seed ottimali, formato da nodi che non hanno ortologhi in tutte le specie e nodi con grado < SIGMA
	public static HashSet<Integer>[] filterNodes(Grafo[] g, HashSet<Integer> allOrto)
	{
		int i=0;
		HashSet<Integer>[] nodiCluster=new HashSet[g.length];
		for(i=0;i<g.length;i++)
		{
			nodiCluster[i]=new HashSet<Integer>();
			OrderedList<Degree> rankScores=new OrderedList<Degree>();
			HashMap<Integer,Nodo> mapNodi=g[i].nodi();
			Set<Integer> keys=mapNodi.keySet();
			Iterator<Integer> it=keys.iterator(); 
			while(it.hasNext())
			{
				int id=it.next();
				int grado=mapNodi.get(id).grado();
				Degree c=new Degree(id, grado);
				rankScores.insertOrdered(c);
			}
			NodoOrdList<Degree> aux=rankScores.getMax();
			while(aux!=null)
			{
				Degree c=aux.getInfo();
				if(c.getDegree()>=SIGMA && allOrto.contains(c.getId()))
					nodiCluster[i].add(c.getId());
				if(!allOrto.contains(c.getId()))
					g[i].removeNode(c.getId());
				aux=aux.getNext();
			}
			//System.out.println(nodiCluster[i].size());
		}
		return nodiCluster;
	}
    
	//Rimozione dall'allineamento del set di nodi allineati con goodness minima
	public static void removeNode(Grafo[] g, Vector<Integer>[] seed)
	{
		int i=0, j=0, k=0;
		int indexRemove=0;
		double minGoodness=10;
		for(j=0;j<seed[0].size();j++)
		{
			double goodness=1.0;
			for(i=0;i<seed.length;i++)
			{
				//Calcolo peso nodo
				double inDegree=0.0;
				HashMap<Integer,Double> adiac=g[i].nodi().get(seed[i].get(j)).getAdiacs();
				for(k=0;k<seed[i].size();k++)
				{
					if(adiac.containsKey(seed[i].get(k)))
						inDegree++;
				}
				double modularity=inDegree/g[i].nodi().get(seed[i].get(j)).grado();
				goodness*=modularity;
			}
			if(goodness<minGoodness)
			{
				indexRemove=j;
				minGoodness=goodness;
			}
                }
		//Rimozione dal seed
		for(i=0;i<seed.length;i++)
			seed[i].remove(indexRemove);
	}
    
	//Estensione dei seed dell'allineamento
	public static void extendSeeds(Grafo[] g, Vector<Integer>[] seed, HashSet<Integer>[] adiacSeed, HashMap mapHomology, double initDens)
	{
		boolean cont=true;
		int j=0, k=0;
		double bestDens=initDens;
		while(cont)
		{
			double[][][] adiacMaps= new double[g.length][][];
			int[][] ids=new int[g.length][];
			buildMaps(g, seed, adiacSeed, ids, adiacMaps);
			for(j=0;j<ids.length;j++)
			{
				if(ids[j].length==0)
					cont=false;
			}
			if(cont)
			{
				int[] align;
				double alignScore=0.0;
				if(PAIR_SCORES)
				{
					GibbsSamplerExtendPairScores gse=new GibbsSamplerExtendPairScores(ITER_EXTEND,ids,adiacMaps);
					align=gse.runGibbs(mapHomology);
					alignScore=gse.getBestScoreLabel();
				}
				else
				{
					GibbsSamplerExtendCOG gse=new GibbsSamplerExtendCOG(ITER_EXTEND,ids,adiacMaps);
					align=gse.runGibbs(mapHomology);
					alignScore=gse.getBestScoreLabel();
				}
				if(alignScore<g.length-1)
					cont=false;
				else
				{
					for(j=0;j<align.length;j++)
						seed[j].add(align[j]);
					double avgDens=modularity(g,seed);
					if(avgDens<bestDens)
					{
						for(j=0;j<align.length;j++)
							seed[j].remove(seed[j].size()-1);
						cont=false;
					}
					else
					{
						bestDens=avgDens;
						for(j=0;j<adiacSeed.length;j++)
						{
							HashMap<Integer,Double> adiac=g[j].nodi().get(seed[j].get(seed[j].size()-1)).getAdiacs();
							Set<Integer> keys=adiac.keySet();
							Iterator<Integer> it=keys.iterator();
							while(it.hasNext())
								adiacSeed[j].add(it.next());
							for(k=0;k<seed[j].size();k++)
								adiacSeed[j].remove(seed[j].get(k));
						}
					}
				}
			}
		}
	}
    
	public static HashSet<Integer> buildHomologiesFromCOG(int numNet, HashMap<Integer,Integer> mapNet, HashMap<Integer,Vector<String>> mapHomology, HashMap<String,Integer> mapNodi) throws Exception
	{
		int i=0;
		HashMap<String, boolean[]> groups=new HashMap<String,boolean[]>();
		HashSet<String> allGroups=new HashSet<String>();
		HashSet<Integer> allOrto=new HashSet<Integer>();
		BufferedReader br=new BufferedReader(new FileReader(homologyFileName));
		String str="";
		while((str=br.readLine())!=null)
		{
			String[] campi=str.split("\t");
			String source=campi[0].trim();
			if(mapNodi.containsKey(source))
			{
				int idSource=mapNodi.get(source);
				Vector<String> listaGruppi=new Vector<String>();
				String[] list=campi[1].substring(1,campi[1].length()-1).split(", ");
				for(i=0;i<list.length;i++)
				{
					listaGruppi.add(list[i]);
					boolean[] species=new boolean[numNet];
					if(groups.containsKey(list[i]))
						species=groups.get(list[i]);
					species[mapNet.get(idSource)]=true;
					groups.put(list[i],species);
				}
				Collections.sort(listaGruppi);
				mapHomology.put(idSource,listaGruppi);
			}
		}
		br.close();
		
		Set<String> keys=groups.keySet();
		Iterator<String> it=keys.iterator();
		while(it.hasNext())
		{
			String id=it.next();
			boolean[] species=groups.get(id);
			int cont=0;
			for(i=0;i<species.length;i++)
			{
				if(species[i])
					cont++;
				else
					break;
			}
			if(cont==numNet)
				allGroups.add(id);
		}
		
		Set<Integer> keys2=mapHomology.keySet();
		Iterator<Integer> it2=keys2.iterator();
		while(it2.hasNext())
		{
			int id=it2.next();
			Vector<String> list=mapHomology.get(id);
			for(i=0;i<list.size();i++)
			{
				String group=list.get(i);
				if(!allGroups.contains(group))
					list.remove(group);
			}
			mapHomology.put(id,list);
			if(list.size()!=0)
				allOrto.add(id);
		}
		
		return allOrto;
	}
	
	public static HashSet<Integer> buildHomologiesFromPairwiseScores(int numNet,HashMap<Integer,Integer> mapNet, HashMap<Integer,HashMap<Integer,Double>> mapHomology, HashMap<String,Integer> mapNodi) throws Exception
	{
		int i=0;
		HashMap<Integer,boolean[]> ortologhi=new HashMap<Integer,boolean[]>();
		HashSet<Integer> allOrto=new HashSet<Integer>();
		BufferedReader br=new BufferedReader(new FileReader(homologyFileName));
		br.readLine();
		String str="";
		while((str=br.readLine())!=null)
		{
			String[] campi=str.split("\t");
			String source=campi[0].trim();
			String dest=campi[1].trim();
			if(mapNodi.containsKey(source) && mapNodi.containsKey(dest))
			{
			int idSource=mapNodi.get(source);
			int idDest=mapNodi.get(dest);
			double weight=Double.parseDouble(campi[2].trim());
			HashMap<Integer,Double> omologhi;
			if(idSource<idDest)
			{
				if(mapHomology.containsKey(idSource))
					omologhi=mapHomology.get(idSource);
				else
					omologhi=new HashMap<Integer,Double>();
				omologhi.put(idDest,weight);
				mapHomology.put(idSource,omologhi);
			}
			else
			{
				if(mapHomology.containsKey(idDest))
					omologhi=mapHomology.get(idDest);
				else
					omologhi=new HashMap<Integer,Double>();
				omologhi.put(idSource,weight);
				mapHomology.put(idDest,omologhi);
			}
		
			boolean[] orto;
			if(ortologhi.containsKey(idSource))
				orto=ortologhi.get(idSource);
			else
				orto=new boolean[numNet];
			orto[mapNet.get(idDest)]=true;
			ortologhi.put(idSource,orto);
		
			if(ortologhi.containsKey(idDest))
				orto=ortologhi.get(idDest);
			else
				orto=new boolean[numNet];
			orto[mapNet.get(idSource)]=true;
			ortologhi.put(idDest,orto);
			}
		}
		br.close();
		//System.out.println(mapHomology);
	
		Set<Integer> keys=ortologhi.keySet();
		Iterator<Integer> it=keys.iterator();
		while(it.hasNext())
		{
			int id=it.next();
			boolean[] orto=ortologhi.get(id);
			int cont=0;
			for(i=0;i<orto.length;i++)
			{
				if(i!=mapNet.get(id) && orto[i])
					cont++;
			}
			if(cont==numNet-1)
				allOrto.add(id);
		}
		return allOrto;
	}
	
	public static void writeAlignments(Grafo[] g, OrderedList<Allineamento> rankAlign) throws Exception
	{
		//System.out.println("\nBEST_ALIGNMENTS:\n");
		NodoOrdList<Allineamento> aux=rankAlign.getMax();
		int cont=1;
		int i=0, j=0;
		while(aux!=null)
		{
			Allineamento a=aux.getInfo();
			//System.out.println(a);
			Vector<Integer>[] mapping=a.getMapping();
			//Memorizzazione allineamento
			BufferedWriter bw=new BufferedWriter(new FileWriter(PATH+"/Alignment"+cont+".txt"));
			bw.write("Total score: "+a.getComplexSize()*a.getIsc()+"\r\n");
			bw.write("Complex size: "+a.getComplexSize()+"\r\n");
			bw.write("Index of Structural Conservation (ISC): "+a.getIsc()+"\r\n"+"\r\n");
			for(j=0;j<mapping.length;j++)
			{
				bw.write(g[j].getName().toUpperCase()+":\r\n");
				bw.write(buildSubgraph(g[j], mapping[j])+"\r\n");
			}
			bw.write("MAPPING:\r\n"+"\r\n");
			for(j=0;j<mapping[0].size();j++)
			{
				for(i=0;i<mapping.length;i++)
				{
					HashMap<Integer,Nodo> listaNodi=g[i].nodi();
					bw.write(listaNodi.get(mapping[i].get(j)).getLabel()+"\t");
				}
				bw.write("\r\n");
			}
			bw.close();
			aux=aux.getNext();
			cont++;
		}
		//System.out.println();
	}
	
	public static void checkOverlap(int numNet, OrderedList<Allineamento> rankAlign)
	{
		int i=0, j=0;
		NodoOrdList<Allineamento> aux=rankAlign.getMax();
		HashSet<Integer> nodiOverlap=new HashSet<Integer>();
		while(aux!=null)
		{
			Vector<Integer>[] mapping=aux.getInfo().getMapping();
			double[] overlapping=new double[numNet];
			for(i=0;i<overlapping.length;i++)
				overlapping[i]=0.0;
			for(i=0;i<mapping.length;i++)
			{
				for(j=0;j<mapping[i].size();j++)
				{
					if(nodiOverlap.contains(mapping[i].get(j)))
						overlapping[i]++;
				}
			}
			double avg=0.0;
			for(i=0;i<overlapping.length;i++)
			{
				overlapping[i]=overlapping[i]/mapping[0].size();
				avg+=overlapping[i];
			}
			avg=avg/overlapping.length;
			if(avg<=OVERLAP)
			{
				for(i=0;i<mapping.length;i++)
				{
					for(j=0;j<mapping[i].size();j++)
						nodiOverlap.add(mapping[i].get(j));
				}
			}	
			else
				rankAlign.delete(aux.getInfo());
			aux=aux.getNext();
		}
	}
    
}