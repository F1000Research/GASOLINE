//GIBBS-SAMPLER per l'allineamento di strutture 3D di proteine

import java.util.*;
import java.sql.*;
import java.io.*;

public class GibbsSamplerSeedCOG
{
	private int numIter;
	private Integer[][] nodi;
	private double bestScoreLabel;
	private double scoreLabel;
	
	public GibbsSamplerSeedCOG(int numIter, HashSet<Integer>[] nodi)
	{
		this.numIter=numIter;
		int i=0;
		this.nodi=new Integer[nodi.length][];
		for(i=0;i<nodi.length;i++)
			this.nodi[i]=nodi[i].toArray(new Integer[nodi[i].size()]);
		this.bestScoreLabel=0.0;
		this.scoreLabel=0.0;
	}
		
	public Vector<Integer>[] runGibbs(HashMap<Integer,Vector<String>> mapHomology)
	{
		int[] allineamento=allineamentoIniziale();
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
			allineamento[indexSost]=selezionaNuovaFinestra(indexSost, prob);
			double scoreAll=scoreAllineamento(allineamento, mapHomology);
			//System.out.println(scoreAll);
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
			if(bestScoreLabel==nodi.length)
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
	
	public double[] calcolaLR(int[] allineamento,int index, HashMap<Integer,Vector<String>> mapHomology)
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
					double sim=orthologJaccard(idNodo,allineamento[j],mapHomology);
					if(sim!=0.0)
						probSelect[i]*=sim;
					else
						probSelect[i]*=0.01;
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
	
	public double scoreAllineamento(int[] allineamento, HashMap<Integer,Vector<String>> mapHomology)
	{
		int i=0, j=0;
		scoreLabel=0.0;
		double scoreAlign=0.0;
		for(i=0;i<allineamento.length-1;i++)
		{
			double sim=orthologJaccard(allineamento[i],allineamento[i+1],mapHomology);
			if(sim!=0.0)
				scoreLabel++;
			scoreAlign+=sim;
		}
		double sim=orthologJaccard(allineamento[i],allineamento[0],mapHomology);
		if(sim!=0.0)
			scoreLabel++;
		scoreAlign+=sim;
		return scoreAlign;
	}
	
	public double getBestScore()
	{
		return bestScoreLabel;
	}
	
	public double orthologJaccard(int a, int b, HashMap<Integer,Vector<String>> mapHomology)
	{
		double orto=0.0;
		if(mapHomology.containsKey(a) && mapHomology.containsKey(b))
		{
			Vector<String> ortoA=mapHomology.get(a);
			Vector<String> ortoB=mapHomology.get(b);
			int size=0;
			int i=0, j=0;
			while(i<ortoA.size() && j<ortoB.size())
			{
				if(ortoA.get(i).compareTo(ortoB.get(j))<0)
				{
					i++;
					size++;
				}
				else if(ortoA.get(i).compareTo(ortoB.get(j))>0)
				{
					j++;
					size++;
				}
				else
				{
					orto++;
					i++;
					j++;
					size++;
				}
			}
			//System.out.println(orto);
			orto=orto/size;
		}
		return orto;
	}
}