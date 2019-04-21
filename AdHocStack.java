public class AdHocStack<T>
{
  private T[] _a = (T[]) new Object[1];
  private int _N = 0;

  public boolean isEmpty()  { return _N == 0;   }
  public int     size()     { return _N;        }
  public int     capacity() { return _a.length; }

  private void _resize( int new_sz )
  {
    if ( _N > new_sz )
      throw new RuntimeException( "Illegal new size of stack's internal strorage" );

    T[] buffer = (T[]) new Object[ new_sz ];
    for ( int i = 0; i < _N; i++ )
      buffer[i] = _a[i];

    _a = buffer;
  }

  public void push( T ele )
  {
    if ( size() == capacity() ) _resize( capacity() * 2 );
    _a[_N++] = ele;
  }

  public T pop()
  {
    T popped = _a[--_N];

    if ( size() > 0 && size() == capacity() / 4 )
      _resize( capacity() / 2 );

    return popped;
  }
}
