package melonslise.spacetest.util;

import java.util.Iterator;

public class ArrayIterator<T> implements Iterator<T>
{
	public int idx = 0;
	public Object[] array;

	public ArrayIterator(Object[] array)
	{
		this.array = array;
	}

	@Override
	public boolean hasNext()
	{
		return this.idx < this.array.length;
	}

	@Override
	public T next()
	{
		return (T) this.array[this.idx++];
	}
}