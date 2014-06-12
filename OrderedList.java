public class OrderedList<E>
{
	private NodoOrdList<E> head;
	private NodoOrdList<E> tail;
    
	public OrderedList()
	{
		head=tail=null;   
	}
    
	public NodoOrdList<E> getMax()
	{
		return head;
	}
    
	public NodoOrdList<E> getMin()
	{
		return tail;
	}
    
	public boolean isEmpty()
	{
		return head==null;
	}
    
	public void clearAll()
	{
		head=tail=null;
	}
    
	public void insertHead(E val)
	{
		if(isEmpty())
			head=tail=new NodoOrdList<E>(val);
		else
		{
			NodoOrdList<E> n=new NodoOrdList<E>(val,head,null);
			head=n;
			head.getNext().setPrev(n);
		}
	}
    
	public void insertTail(E val)
	{
		if(isEmpty())
			head=tail=new NodoOrdList<E>(val);
		else 
		{
			NodoOrdList<E> n=new NodoOrdList<E>(val,null,tail);
			tail=n;
			tail.getPrev().setNext(n);
		}
	}
    
	public void insertOrdered(E key)
	{
		if(isEmpty())
			head=tail=new NodoOrdList<E>(key);
		else 
		{
			if(((Comparable)head.getInfo()).compareTo(key)<=0)
				insertHead(key);
			else if(((Comparable)tail.getInfo()).compareTo(key)>0)
				insertTail(key);
			else 
			{
				NodoOrdList<E> aux=head;
				while(((Comparable)aux.getInfo()).compareTo(key)>0)
					aux=aux.getNext();
				NodoOrdList<E> n=new NodoOrdList<E>(key,aux,aux.getPrev());
				aux.getPrev().setNext(n);
				aux.setPrev(n);
			}
		}
	}
    
	public void deleteHead()
	{
		if(head==tail)
			head=tail=null;
		else
		{
			head=head.getNext();
			head.setPrev(null);
		}
	}
    
	public void deleteTail()
	{
		if(head==tail)
			head=tail=null;
		else
		{
			tail=tail.getPrev();
			tail.setNext(null);
		}
	}
    
	public void delete(E key)
	{
		try
		{
			if(isEmpty())
				throw new Exception("Error! Empty list!");
			else
			{
				if(((Comparable)head.getInfo()).compareTo(key)==0)
					deleteHead();
				else if(((Comparable)tail.getInfo()).compareTo(key)==0)
					deleteTail();
				else 
				{
					NodoOrdList<E> aux=head;
					while(aux!=null && ((Comparable)aux.getInfo()).compareTo(key)!=0)
						aux=aux.getNext();
					if(aux==null)
						throw new Exception("Error! Element "+key+" doesn't exist!");
					else
					{
						aux.getPrev().setNext(aux.getNext());
						aux.getNext().setPrev(aux.getPrev());
					}
				}
			}
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
    
	public E get(int pos)
	{
		int i=1;
		NodoOrdList<E> aux=head;
		for(i=1;i<pos;i++)
			aux=aux.getNext();
		return aux.getInfo();
	}
    
	public String toString()
	{
		String str="";
		if(isEmpty())
			str="La lista attualmente e' vuota \n";
		else 
		{
			str="Gli elementi attualmente presenti nella lista sono: \n";
			NodoOrdList<E> aux=head;
			while(aux!=null)
			{
				str+=aux.getInfo().toString()+"\t";
				aux=aux.getNext();
			}
			str+="\n";
		}
		return str;
	}	
    
}