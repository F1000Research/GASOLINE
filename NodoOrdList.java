public class NodoOrdList<E>
{
	private E info;
	private NodoOrdList<E> next;
	private NodoOrdList<E> prev;
    
	public NodoOrdList(E val)
	{
		this(val,null,null);
	}
    
	public NodoOrdList(E val,NodoOrdList<E> n, NodoOrdList<E> p)
	{
		info=val;
		next=n;
		prev=p;
	}
    
	public E getInfo()
	{
		return info;
	}
    
	public NodoOrdList<E> getNext()
	{
		return next;
	}
    
	public NodoOrdList<E> getPrev()
	{
		return prev;
	}
    
	public void setInfo(E val)
	{
		info=val;
	}
    
	public void setNext(NodoOrdList<E> n)
	{
		next=n;
	}
    
	public void setPrev(NodoOrdList<E> p)
	{
		prev=p;
	}
}