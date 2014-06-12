import java.util.*;
import java.io.*;

public class GibbsSamplerSeedPairScores
{
	private int numIter;
	private Integer[][] nodi;
	private double bestScoreLabel;
	private double scoreLabel;
	
	public GibbsSamplerSeedPairScores(int numIter, HashSet<Integer>[] nodi)
	{
		this.numIter=numIter;
		int i=0;
		this.nodi=new Integer[nodi.length][];
		for(i=0;i<nodi.length;i++)
			this.nodi[i]=nodi[i].toArray(new Integer[nodi[i].size()]);
		this.bestScoreLabel=0.0;
		this.scoreLabel=0.0;
	}
		
	public Vector<Integer>[] runGibbs(HashMap<Integer,HashMap<Integer,Double>> mapHomology)
	{
		int[] allineamento=allineamentoIniziale();
		int oldAlign=0;
		int cont=0;
		int indIter=0;
		int indexSost=0;
		int bestIter=0;
		double bestScore=0.0;
		Vector<Integer>[] bestAlign=new Vector[nodi.length];
		int i=0, j=0;
		for(i=0;i<bestAlign.length;i++)
			bestAlign[i]=new Vector<Integer>();
		for(indIter=0;indIter<numIter;indIter++)
		{
			indexSost=(int)(Math.random()*nodi.length);
			double[] prob =calcolaLR(allineamento, indexSost, mapHomology);
			oldAlign=allineamento[indexSost];
			allineamento[indexSost]=selezionaNuovaFinestra(indexSost, prob);
			if(oldAlign==allineamento[indexSost])
				cont++;
			else
				cont=0;
			double scoreAll=scoreAllineamento(allineamento, mapHomology);
			if(indIter==0 || scoreAll>bestScore)
			{
				bestScoreLabel=scoreLabel;
				bestScore=scoreAll;
				for(i=0;i<allineamento.length;i++)
				{
					bestAlign[i]=new Vector<Integer>();
					bestAlign[i].add(allineamento[i]);
				}
				bestIter=indIter+1;
			}
			if(bestScoreLabel==nodi.length && cont==nodi.length*2)
				break;
		}
		return bestAlign;
	}
	
	public int[] allineamentoIniziale()
	{
		int[] allineamento=new int[nodi.length];
		int i=0;
		for(i=0;i<nodi.length;i++)
		{
			int row=(int)(Math.random()*nodi[i].length);
			allineamento[i]=nodi[i][row];
		}
		return allineamento;
	}
    
	public double[] calcolaLR(int[] allineamento,int index, HashMap<Integer,HashMap<Integer, Double>> mapHomology)
	{
		double[] probSelect=new double[nodi[index].length];
		int i=0, j=0;
		int idNodo;
		double denom=0.0;
		for(i=0;i<nodi[index].length;i++)
		{ 
			idNodo=nodi[index][i];
			probSelect[i]=1.0;
			for(j=0;j<nodi.length;j++)
			{
				if(j!=index)
				{
					if(idNodo<allineamento[j] && mapHomology.containsKey(idNodo))
					{
						HashMap<Integer,Double> omologhi=mapHomology.get(idNodo);
						if(omologhi.containsKey(allineamento[j]))
							probSelect[i]*=omologhi.get(allineamento[j]);
					}
					else if(mapHomology.containsKey(allineamento[j]))
					{
						HashMap<Integer,Double> omologhi=mapHomology.get(allineamento[j]);
						if(omologhi.containsKey(idNodo))
							probSelect[i]*=omologhi.get(idNodo);
					}
				}
			}
			denom+=probSelect[i];
		}
		for(i=0;i<probSelect.length;i++)
			probSelect[i]=probSelect[i]/denom;
		return probSelect;
	}
	
	public int selezionaNuovaFinestra(int index, double[] prob)
	{
		int i=0, indFinestra=0;
		boolean trovato=false;
		double rand=Math.random();
		while(i<prob.length && !trovato)
		{
			rand=rand-prob[i];
			if(rand<0)
			{
				indFinestra=i;
				trovato=true;
			}
			i++;
		}
		return nodi[index][indFinestra];
	}
	
	public double scoreAllineamento(int[] allineamento, HashMap<Integer,HashMap<Integer,Double>> mapHomology)
	{
		int i=0, j=0;
		scoreLabel=0.0;
		double scoreAlign=0.0;
		for(i=0;i<allineamento.length-1;i++)
		{
			if(allineamento[i]<allineamento[i+1] && mapHomology.containsKey(allineamento[i]))
			{
				HashMap<Integer,Double> omologhi=mapHomology.get(allineamento[i]);
				if(omologhi.containsKey(allineamento[i+1]))
				{
					scoreLabel++;
					scoreAlign+=omologhi.get(allineamento[i+1]);
				}
			}
			else if(mapHomology.containsKey(allineamento[i+1]))
			{
				HashMap<Integer,Double> omologhi=mapHomology.get(allineamento[i+1]);
				if(omologhi.containsKey(allineamento[i]))
				{
					scoreLabel++;
					scoreAlign+=omologhi.get(allineamento[i]);
				}
			}
		}
		if(allineamento[i]<allineamento[0] && mapHomology.containsKey(allineamento[i]))
		{
			HashMap<Integer,Double> omologhi=mapHomology.get(allineamento[i]);
			if(omologhi.containsKey(allineamento[0]))
			{
				scoreLabel++;
				scoreAlign+=omologhi.get(allineamento[0]);
			}
		}
		else if(mapHomology.containsKey(allineamento[0]))
		{
			HashMap<Integer,Double> omologhi=mapHomology.get(allineamento[0]);
			if(omologhi.containsKey(allineamento[i]))
			{
				scoreLabel++;
				scoreAlign+=omologhi.get(allineamento[i]);
			}
		}
		return scoreAlign;
	}
	public double getBestScore()
	{
		return bestScoreLabel;
	}
}