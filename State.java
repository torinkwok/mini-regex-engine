import java.util.*;

public class State implements Cloneable
{
  private class AdHocNullable<T> implements Cloneable
  {
    private T _value;

    public AdHocNullable()         { _value = null; }
    public AdHocNullable( T init ) { _value = init; }

    public void    set( T v ) { _value = v; }
    public boolean hasValue() { return _value != null; }

    public T value()                          { return _value; }
    public T valueOrDefault( T defaultValue ) { return _value == null ? defaultValue : _value; }

    @Override
    public AdHocNullable<T> clone()
    {
      AdHocNullable<T> cloned;
      cloned = hasValue() ? new AdHocNullable() : new AdHocNullable( value() );
      return cloned;
    }

    @Override
    public int hashCode() { return Objects.hashCode( _value ); }

    public boolean equals( Object o )
    {
      if ( this == o )                  return true;
      if ( o == null )                  return false;
      if ( getClass() != o.getClass() ) return false;

      AdHocNullable<T> nullable = ( AdHocNullable<T> )o;
      if ( hasValue() != nullable.hasValue() )      return false;
      if ( !( hasValue() || nullable.hasValue() ) ) return true;

      return value().equals( nullable.value() );
    }
  }

  private final AdHocNullable<Integer>      _Sn;
  private final AdHocNullable<Set<Integer>> _nfaStatesSet;

  public final boolean isSubsetState;

  public State( Set<Integer> statesSet )
  {
    _Sn = new AdHocNullable();

    _nfaStatesSet = new AdHocNullable( statesSet );
    _nfaStatesSet.set( statesSet );

    isSubsetState = true;
  }

  public boolean equals( Object o )
  {
    if ( this == o ) return true;
    if ( o == null ) return false;
    if ( getClass() != o.getClass() ) return false;

    State state = ( State )o;

    if ( isSubsetState != state.isSubsetState ) return false;

    if ( isSubsetState ) {
      return _nfaStatesSet.equals( state._nfaStatesSet );
    } else {
      return _Sn.value() == state._Sn.value();
    }
  }

  @Override
  public State clone()
  {
    State cloned;
    cloned = isSubsetState ? new State( _nfaStatesSet.value() ) : new State( _Sn.value() );
    return cloned;
  }

  @Override
  public int hashCode() { return Objects.hash( _Sn, _nfaStatesSet ); }

  public State( int sn )
  {
    _Sn = new AdHocNullable( sn );
    _Sn.set( sn );

    _nfaStatesSet = new AdHocNullable();
    isSubsetState = false;
  }

  public static void main( String args[] )
  {
    State state_0 = new State( 14 );
    State state_1 = new State( 14 );

    State state_2 = new State( new HashSet<>( Arrays.asList( 2, 6, 6 ) ) );
    State state_3 = state_2.clone();
    State state_4 = new State( new HashSet<>( Arrays.asList( 2, 6, 7 ) ) );

    assert state_0.hashCode() == state_1.hashCode();
    assert state_0.hashCode() != state_2.hashCode();
    assert state_2.hashCode() == state_3.hashCode();
    assert state_3.hashCode() != state_4.hashCode();

    assert state_0.equals( state_1 );
    assert !state_0.equals( state_2 );

    assert state_2.equals( state_3 );
    assert !state_2.equals( state_4 );
  }
}
