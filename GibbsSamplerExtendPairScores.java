import java.util.*;
import java.io.*;

public class GibbsSamplerExtendPairScores
{
	private int numIter;
	private int[][] ids;
	private double[][][] adiacMaps;
	private double scoreLabel;
	private double bestScoreLabel;
	
	public GibbsSamplerExtendPairScores(int numIter, int[][] ids, double[][][] adiacMaps)
	{
		this.numIter=numIter;
		this.ids=ids;
		this.adiacMaps=adiacMaps;
		this.scoreLabel=0.0;
		this.bestScoreLabel=0.0;
	}
		
	public int[] runGibbs(HashMap<Integer,HashMap<Integer,Double>> mapHomology)
	{
		int[] allineamento=allineamentoIniziale();
		int indIter=0;
		int indexSost=0;
		double bestScore=0.0;
		int bestIter=0;
		int[] bestAlign=new int[ids.length];
		int i=0, j=0;
		int oldAlign=0;
		int cont=0;
		for(indIter=0;indIter<numIter;indIter++)
		{
			indexSost=(int)(Math.random()*ids.length);
			double[] prob =calcolaLR(allineamento, indexSost, mapHomology);
			oldAlign=allineamento[indexSost];
			allineamento[indexSost]=selezionaNuovaFinestra(indexSost, prob);
			if(oldAlign==allineamento[indexSost])
				cont++;
			else
				cont=0;
			double scoreAll=scoreAllineamento(allineamento, mapHomology);
			if(scoreAll>bestScore)
			{
				bestScoreLabel=scoreLabel;
				bestScore=scoreAll;
				for(i=0;i<allineamento.length;i++)
					bestAlign[i]=ids[i][allineamento[i]];
				bestIter=indIter+1;
			}
			if(bestScoreLabel==ids.length && cont==ids.length*2)
				break;
		}
		return bestAlign;
	}
    
	public int[] allineamentoIniziale()
	{
		int[] allineamento=new int[ids.length];
		int i=0, j=0, k=0;
		for(i=0;i<ids.length;i++)
		{
			int row=(int)(Math.random()*ids[i].length);
			allineamento[i]=row;
		}
		return allineamento;
	}
	
	public double[] calcolaLR(int[] allineamento,int index, HashMap<Integer,HashMap<Integer,Double>> mapHomology)
	{
		double[] probLabel=new double[ids[index].length];
		double[] probDegree=new double[ids[index].length];
		double scoreDegree=0.0;
		int i=0, j=0;
		int k=0;
		double denom=0.0;
		for(j=0;j<ids[index].length;j++)
		{ 
			probLabel[j]=1.0;
			probDegree[j]=1.0;
			for(i=0;i<ids.length;i++)
			{
				if(i!=index)
				{
					if(ids[i][allineamento[i]]<ids[index][j] && mapHomology.containsKey(ids[i][allineamento[i]]))
					{
						HashMap<Integer,Double> omologhi=mapHomology.get(ids[i][allineamento[i]]);
						if(omologhi.containsKey(ids[index][j]))
							probLabel[j]*=omologhi.get(ids[index][j]);
					}
					else if(mapHomology.containsKey(ids[index][j]))
					{
						HashMap<Integer,Double> omologhi=mapHomology.get(ids[index][j]);
						if(omologhi.containsKey(ids[i][allineamento[i]]))
							probLabel[j]*=omologhi.get(ids[i][allineamento[i]]);
					}
					scoreDegree=0.0;
					for(k=0;k<adiacMaps[index][j].length;k++)
					{
						//if(adiacMaps[i][allineamento[i]][k]==0.0 && adiacMaps[index][j][k]==0.0)
							//scoreDegree++;
						if(adiacMaps[i][allineamento[i]][k]!=0.0 && adiacMaps[index][j][k]!=0.0)
							scoreDegree+=((adiacMaps[i][allineamento[i]][k]+1)*(adiacMaps[index][j][k]+1));
					}
				}
				if(scoreDegree==0.0)
					scoreDegree+=0.1;
				probDegree[j]*=scoreDegree;
			}
			denom+=probDegree[j];
		}
		for(i=0;i<probLabel.length;i++)
			probDegree[i]=probDegree[i]/denom;
        
		denom=0.0;
		for(i=0;i<probLabel.length;i++)
		{
			probLabel[i]=probLabel[i]*probDegree[i];
			denom+=probLabel[i];
		}
		for(i=0;i<probLabel.length;i++)
			probLabel[i]=probLabel[i]/denom;
		return probLabel;
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
		return indFinestra;
	}
	
	public double scoreAllineamento(int[] allineamento, HashMap<Integer,HashMap<Integer,Double>> mapHomology)
	{
		int i=0, j=0, k=0;
		
		//Calcolo il punteggio relativo alla corrispondenza tra etichette (Circular Sum)
		scoreLabel=0.0;
		double scoreLabel2=0.0;
		double scoreDegree=0.0;
		double parzScoreDegree=0.0;
		for(i=0;i<ids.length-1;i++)
		{
			if(ids[i][allineamento[i]]<ids[i+1][allineamento[i+1]] && mapHomology.containsKey(ids[i][allineamento[i]]))
			{
				HashMap<Integer,Double> omologhi=mapHomology.get(ids[i][allineamento[i]]);
				if(omologhi.containsKey(ids[i+1][allineamento[i+1]]))
				{
					scoreLabel++;
					scoreLabel2+=omologhi.get(ids[i+1][allineamento[i+1]]);
				}
			}
			else if(mapHomology.containsKey(ids[i+1][allineamento[i+1]]))
			{
				HashMap<Integer,Double> omologhi=mapHomology.get(ids[i+1][allineamento[i+1]]);
				if(omologhi.containsKey(ids[i][allineamento[i]]))
				{
					scoreLabel++;
					scoreLabel2+=omologhi.get(ids[i][allineamento[i]]);
				}
			}
			parzScoreDegree=0.0;
			for(k=0;k<adiacMaps[i][allineamento[i]].length;k++)
			{
				//if(adiacMaps[i][allineamento[i]][k]==0.0 && adiacMaps[i+1][allineamento[i+1]][k]==0.0)
					//parzScoreDegree++;
				if(adiacMaps[i][allineamento[i]][k]!=0.0 && adiacMaps[i+1][allineamento[i+1]][k]!=0.0)
					parzScoreDegree+=((adiacMaps[i][allineamento[i]][k]+1)*(adiacMaps[i+1][allineamento[i+1]][k]+1));
			}
			if(parzScoreDegree==0.0)
				parzScoreDegree+=0.5;
			scoreDegree+=parzScoreDegree;
		}
		if(ids[i][allineamento[i]]<ids[0][allineamento[0]] && mapHomology.containsKey(ids[i][allineamento[i]]))
		{
			HashMap<Integer,Double> omologhi=mapHomology.get(ids[i][allineamento[i]]);
			if(omologhi.containsKey(ids[0][allineamento[0]]))
			{
				scoreLabel++;
				scoreLabel2+=omologhi.get(ids[0][allineamento[0]]);
			}
		}
		else if(mapHomology.containsKey(ids[0][allineamento[0]]))
		{
			HashMap<Integer,Double> omologhi=mapHomology.get(ids[0][allineamento[0]]);
			if(omologhi.containsKey(ids[i][allineamento[i]]))
			{
				scoreLabel++;
				scoreLabel2+=omologhi.get(ids[i][allineamento[i]]);
			}
		}
		parzScoreDegree=0.0;
		for(k=0;k<adiacMaps[i][allineamento[i]].length;k++)
		{
			//if(adiacMaps[i][allineamento[i]][k]==0.0 && adiacMaps[0][allineamento[0]][k]==0.0)
				//parzScoreDegree++;
			if(adiacMaps[i][allineamento[i]][k]!=0.0 && adiacMaps[0][allineamento[0]][k]!=0.0)
				parzScoreDegree+=((adiacMaps[i][allineamento[i]][k]+1)*(adiacMaps[0][allineamento[0]][k]+1));
		}
		if(parzScoreDegree==0.0)
			parzScoreDegree+=0.5;
		scoreDegree+=parzScoreDegree;
		if(scoreLabel2==0.0)
			scoreLabel2=0.1;
		//System.out.println(scoreLabel);
		//System.out.println(scoreDegree);
		return scoreLabel2*scoreDegree;
	}
	
	public double getBestScoreLabel()
	{
		return bestScoreLabel;
	}
}