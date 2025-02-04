import java.util.*;

final class NFA implements Cloneable
{
  private class AdHocStack<T>
  {
    private T[] _a = (T[]) new Object[1];
    private int _N = 0;

    boolean isEmpty()  { return _N == 0;   }
    int     size()     { return _N;        }
    int     capacity() { return _a.length; }

    private void _resize( int new_sz )
    {
      if ( _N > new_sz )
        throw new RuntimeException( "Illegal new size of stack's internal strorage" );

      T[] buffer = (T[]) new Object[ new_sz ];
      for ( int i = 0; i < _N; i++ )
        buffer[i] = _a[i];

      _a = buffer;
    }

    void push(T ele)
    {
      if ( size() == capacity() ) _resize( capacity() * 2 );
      _a[_N++] = ele;
    }

    T pop()
    {
      T popped = _a[--_N];

      if ( size() > 0 && size() == capacity() / 4 )
        _resize( capacity() / 2 );

      return popped;
    }
  }

  private State start;
  private State end;

  private final Vector<Vector<Input>> transtbl;

  private Set<Input> inputs = new HashSet<>();

  @Override
  public NFA clone() { return new NFA( this ); }

  public NFA( NFA src )
  {
    Vector<Vector<Input>> cloned_transtbl = new Vector<>( src.count() );
    for ( int i = 0; i < src.count(); i++ )
    {
      Vector<Input> src_r = src.transtbl.get( i );
      Vector<Input> cloned_r = new Vector<>( src.count() );

      for ( int j = 0; j < src.count(); j++ )
        cloned_r.add( src_r.get( j ) );

      cloned_transtbl.add( cloned_r );
    }

    this.transtbl = cloned_transtbl;
    this.start    = src.start;
    this.end      = src.end;
    this.inputs   = src.inputs;
  }

  public NFA( int size, int start, int end )
  {
    assert( _isLegalState( start ) );
    assert( _isLegalState( end ) );

    this.transtbl = new Vector<>( size );
    this.start    = new State( start );
    this.end      = new State( end );

    // Initialize transtbl with an "empty graph",
    // no transitions between its states

    for ( int i = 0; i < size; i++ )
    {
      Vector<Input> row = new Vector<>( size );
      for ( int j = 0; j < size; j++ ) row.add( Input.NONE );

      transtbl.add( row );
    }
  }

  public int count() { return transtbl.size(); }
  private boolean _isLegalState( int s ) { return s >= 0 || s < count(); }

  private Map<Map<State, Input>, State>
  _subsetConstruction( State cur_dstate, final Map<Map<State, Input>, State> partial_dfa_rep )
  {
    if ( partial_dfa_rep == null )
      throw new IllegalArgumentException( "partial_dfa_rep must not be null" );

    Set<State> next_dstates = new HashSet<>();

    for ( Input in : this.inputs )
    {
      HashSet buffer = new HashSet();

      for ( State cs : State.stateStates( cur_dstate.nfaStatesSet() ) )
      {
        buffer.addAll( _nextNStates( new HashSet(){{ add( cs ); }}, in ) );
        buffer.addAll( _epsClosure( buffer ) );
      }

      State next_dstate = new State( State.integerStates( buffer ) );
      HashMap from_key = new HashMap(){{ put( cur_dstate, in ); }};

      if ( !partial_dfa_rep.containsKey( from_key ) )
      {
        partial_dfa_rep.put( from_key, next_dstate );
        next_dstates.add( next_dstate );
      }
    }

    for ( State ds : next_dstates )
      partial_dfa_rep.putAll( _subsetConstruction( ds, partial_dfa_rep ) );

    return partial_dfa_rep;
  }

  public DFA dfa()
  {
    // It starts by creating the initial state for the DFA. Since
    // an initial state is really the NFA's initial state plus
    // all the states reachable by *eps* transitions from it. The
    // DFA initial state is the eps-closure of the NFA's initial
    // state.

    State dfa_start_state = new State(
      State.integerStates( _epsClosure( new HashSet(){{ add( start ); }} ) ) );

    DFA dfa = new DFA();
    dfa.start = dfa_start_state;
    dfa.finalMarks.add( this.end );

    Map<Map<State, Input>, State> dfa_rep = _subsetConstruction( dfa.start, new HashMap() );

    for ( Map.Entry<Map<State, Input>, State> entry : dfa_rep.entrySet() )
    {
      for ( Map.Entry<State, Input> keyentry : entry.getKey().entrySet() )
        dfa.addTransition( keyentry.getKey(), entry.getValue(), keyentry.getValue() );
    }
    return dfa;
  }

  public void addTransition( State from, State to, Input in ) { addTransition( from.n(), to.n(), in ); }
  public void addTransition( State from, int   to, Input in ) { addTransition( from.n(), to,     in ); }
  public void addTransition( int   from, State to, Input in ) { addTransition( from,     to.n(), in ); }
  public void addTransition( int   from, int   to, Input in )
  {
    assert( _isLegalState( from ) );
    assert( _isLegalState( to ) );

    Vector<Input> row = transtbl.get( from );
    row.setElementAt( in, to );

    if ( in != Input.EPS )
      this.inputs.add( in );
  }

  public void show()
  {
    System.out.println( String.format( "This NFA got %d states: s0 - s%d", count(), count() - 1 ) );
    System.out.println( String.format( "The initial state is " + start ) );
    System.out.println( String.format( "The final state is " + end ) );

    for ( int from = 0; from < count(); from++ )
    {
      for ( int to = 0; to < count(); to++ )
      {
        Vector<Input> row = transtbl.get( from );
        Input in = row.get( to );

        if ( in != Input.NONE )
        {
          System.out.print( String.format( "Transitions from s%d to s%d on input ", from, to ) );

          if   ( in == Input.EPS ) { System.out.println( in.v ); }
          else                     { System.out.println( "'" + in.v + "'" ); }
        }
      }
    }
  }

  /** Appends a new, empty state to the NFA.
   */
  private void _appendEmptyState()
  {
    Vector<Input> newrow = new Vector<>( count() + 1 );
    for ( int i = 0; i < count(); i++ ) { newrow.add( Input.NONE ); }

    transtbl.add( newrow );

    for ( int i = 0; i < count(); i++ )
    {
      Vector<Input> ri = transtbl.get( i );
      ri.add( Input.NONE );
    }

    this.end = new State( this.end.n() + 1 );
  }

  /** Renames all the NFA's states:
   *
   *  For each NFA state: number += shift. Functionally, this
   *  doesn't affect the NFA, it only makes it larger and renames
   *  its states.
   */
  private void _shiftStates( int shift )
  {
    if ( shift < 1 ) { return; }

    for ( int i = 0; i < shift; i++ )
    {
      Vector<Input> newrow = new Vector<>( count() + shift );
      for ( int j = 0; j < count(); j++ ) { newrow.add( Input.NONE ); }

      transtbl.insertElementAt( newrow, 0 );
    }

    for ( int i = 0; i < count(); i++ )
    {
      Vector<Input> ri = transtbl.get( i );
      for ( int j = 0; j < shift; j++ ) { ri.insertElementAt( Input.NONE, 0 ); }
    }

    this.start = new State( this.start.n() + shift );
    this.end   = new State( this.end.n() + shift );
  }

  /** Fills states 0 up to src.count() with src's states.
   */
  private void _fillStates( NFA nfa )
  {
    NFA src = nfa.clone();
    NFA dst = this;
    int srcsz = src.count();

    for ( int i = 0; i < srcsz; i++ )
    {
      for ( int j = 0; j < srcsz; j++ )
      {
        Vector<Input> rsrc = src.transtbl.get( i );
        Vector<Input> rdst = dst.transtbl.get( i );

        rdst.setElementAt( rsrc.get( j ), j );
      }
    }

    for ( Input in : src.inputs )
      this.inputs.add( in );
  }

  public void dumpInternalTranstbl()
  {
    System.out.println( "====================" );
    System.out.println( "Initial State: " + start );
    System.out.println( "  Final State: " + end );
    System.out.println( "--------------------" );
    System.out.println();

    System.out.print( "  " );
    for ( int i = 0; i < count(); i++ ) { System.out.print( " " + i % 10 ); }
    System.out.print( "\n\n" );

    for ( int i = 0; i < count(); i++ )
    {
      Vector<Input> r = transtbl.get( i );
      System.out.print( i % 10 + " " );

      for ( int j = 0; j < count(); j++ )
      {
        char c;
        Input in = r.get( j );
        if      ( in == Input.NONE ) { c = '-';  }
        else                         { c = in.v; }

        System.out.print( String.format( " %c", c ) );
      }

      System.out.println();
    }

    System.out.println();
  }

  public static NFA buildNFAAlternation( NFA n1, NFA n2 )
  {
    NFA nfa1 = n1.clone();
    NFA nfa2 = n2.clone();

    //        +-----------------+
    //        | .-.         .-. |
    //     +-->(   )  N(s) (   )+--+
    //     |  | `-'         `-' |  |
    //     |  +-----------------+  |
    //     |                       v
    //    .-.                    +---+
    //   ( S )                   | F |
    //    `-'                    +---+
    //     |  +-----------------+  ^
    //     |  | .-.         .-. |  |
    //     +--|(   )  N(t) (   )|--+
    //        | `-'         `-' |
    //        +-----------------+
    //
    // The new nfaUnion contain all the states in nfa1 and nfa2,
    // plus a new initial and final states.  First will come the
    // new initial state, then nfa1's states, then nfa2's states,
    // then the new final state.

    nfa1._shiftStates( 1 );            // make room for the new initial state
    nfa2._shiftStates( nfa1.count() ); // make room for nfa1's states

    NFA nfaUnion = new NFA( nfa2 );   // create a new nfa and initialize it
    nfaUnion._fillStates( nfa1 );      // nfa1's states take their places in the new nfa

    nfaUnion.addTransition( 0, nfa1.start, Input.EPS );
    nfaUnion.addTransition( 0, nfa2.start, Input.EPS );
    nfaUnion.start = State.ZERO;

    nfaUnion._appendEmptyState();
    nfaUnion.end = new State( nfaUnion.count() - 1 );

    nfaUnion.addTransition( nfa1.end, nfaUnion.end, Input.EPS );
    nfaUnion.addTransition( nfa2.end, nfaUnion.end, Input.EPS );

    return nfaUnion;
  }

  public static NFA buildNFAConcatenation( NFA n1, NFA n2 )
  {
    NFA nfa1 = n1.clone();
    NFA nfa2 = n2.clone();

    // Make room for nfa1's states.  First will come nfa1, then
    // nfa2 (nfa2's initial state would be overlapped with nfa1's
    // final state
    //
    nfa2._shiftStates( nfa1.count() - 1 );

    NFA nfaConcat = new NFA( nfa2 );

    // nfa1's states take their places in nfaConcat NOTE: nfa1's
    // final state overwrites nfa2's initial state, thus we get
    // the desired merge automagically (the transition from
    // nfa2's initial state now transits from nfa1's final state)
    // 
    nfaConcat._fillStates( nfa1 );

    // Set the new initial state (the final state stays nfa2's
    // final state, and was already copied)
    //
    nfaConcat.start = nfa1.start;

    return nfaConcat;
  }

  public static NFA buildNFAKleeneStar( NFA n )
  {
    NFA nfa = n.clone();

    nfa._shiftStates( 1 );

    NFA nfaKleeneStar = new NFA( nfa );
    nfaKleeneStar._fillStates( nfa );
    nfaKleeneStar._appendEmptyState();

    nfaKleeneStar.start = State.ZERO;
    nfaKleeneStar.end = new State( nfaKleeneStar.count() - 1 );

    nfaKleeneStar.addTransition( 0, nfa.start, Input.EPS );
    nfaKleeneStar.addTransition( 0, nfaKleeneStar.end, Input.EPS );
    nfaKleeneStar.addTransition( nfa.end, nfa.start, Input.EPS );
    nfaKleeneStar.addTransition( nfa.end, nfaKleeneStar.end, Input.EPS );

    return nfaKleeneStar;
  }

  public static NFA buildNFABasic( Input in )
  {
    NFA nfa = new NFA( 2, 0, 1 );
    nfa.addTransition( 0, 1, in );
    return nfa;
  }

  /** Given N - an NFA and T - a set of NFA states, we would like
   *  to know which states in N are reachable from states T by
   *  *eps* transitions. eps-closure is an algorithm that answers
   *  this question.
   *
   *  inputs: T - set of NFA states
   *  output: eps-closure(T) - states reachable from T by eps transitions
   */
  public Set<State> _epsClosure( Set<State> T )
  {
    // This algorithm iteratively finds all the states reachable
    // by *eps* transitions from the states T. First, the states
    // T themselves are added to the output. Then, one by one the
    // states are checked for *eps* transitions, and the states
    // these transitions lead to are also added to the output,
    // and are pushed onto the stack (in order to be checked for
    // *eps* transitions).
    //
    // The process proceeds iteratively, until no more states can be
    // reached with *eps* transitions only. For instance, for the
    // (s|t)*stt NFA above, eps-closure({0}) = {0, 1, 2, 4, 7},
    // eps-closure({8, 9}) = {8, 9}, etc..

    Set<State> closure = new HashSet<>();
    if ( T.isEmpty() ) return closure;

    AdHocStack<State> stack = new AdHocStack<>();

    for ( State st : T )
    {
      stack.push( st );

      while ( !stack.isEmpty() )
      {
        st = stack.pop();

        Vector<Input> r = transtbl.get( st.n() );
        Set<State> U = new HashSet<>();

        U.add( st );

        for ( int c = 0; c < count(); c++ )
          if ( r.get( c ) == Input.EPS )
            U.add( new State( c ) );

        for ( State su : U )
        {
          if ( !closure.contains( su ) )
          {
            closure.add( su );
            stack.push( su );
          }
        }
      }
    }

    return closure;
  }

  /** Given T - a set of NFA states, and A - an input, we would
   *  like to know which states in the NFA are reachable from T
   *  with the input A.
   */
  public Set<State> _nextNStates( Set<State> T, Input A )
  {
    // The function traverses the set T, and looks for
    // transitions on the given input, returning the states that
    // can be reached. It doesn't take into account the *eps*
    // transitions from those states - there's eps-closure
    // algorithm for that.

    Set<State> states = new HashSet<>();

    if ( A == Input.EPS || A == Input.NONE )
      return states;

    for ( State st : T )
    {
      Vector<Input> r = transtbl.get( st.n() );
      for ( int c = 0; c < count(); c++ )
      {
        Input in = r.get( c );
        if ( in == Input.EPS || in == Input.NONE ) continue;
        if ( in.equals( A ) )                      states.add( new State( c ) );
      }
    }

    return states;
  }

  public static void main( String args[] )
  {
    NFA nfa = new NFA( 11, 0, 10 ); 

    nfa.addTransition( 0,  1, Input.EPS ); 
    nfa.addTransition( 0,  7, Input.EPS ); 
    nfa.addTransition( 1,  2, Input.EPS ); 
    nfa.addTransition( 1,  4, Input.EPS );
    nfa.addTransition( 2,  3, new Input( 'a' ) );
    nfa.addTransition( 4,  5, new Input( 'b' ) );
    nfa.addTransition( 3,  6, Input.EPS ); 
    nfa.addTransition( 5,  6, Input.EPS ); 
    nfa.addTransition( 6,  1, Input.EPS ); 
    nfa.addTransition( 6,  7, Input.EPS ); 
    nfa.addTransition( 7,  8, new Input( 'a' ) );
    nfa.addTransition( 8,  9, new Input( 'b' ) );
    nfa.addTransition( 9, 10, new Input( 'b' ) );
   
    nfa.show();

    ///

    nfa.dumpInternalTranstbl();

    nfa._appendEmptyState(); nfa.dumpInternalTranstbl();
    nfa._shiftStates( 3 );   nfa.dumpInternalTranstbl();

    NFA anotherNFA = new NFA( 4, 0, 3 );

    anotherNFA.addTransition( 0, 1, new Input( 'a' ) );
    anotherNFA.addTransition( 0, 0, new Input( 'b' ) );
    anotherNFA.addTransition( 1, 2, new Input( 'b' ) );
    anotherNFA.addTransition( 2, 3, new Input( 'b' ) );
    anotherNFA.addTransition( 2, 1, new Input( 'a' ) );
    anotherNFA.addTransition( 3, 1, new Input( 'a' ) );

    anotherNFA.dumpInternalTranstbl();
    nfa._fillStates( anotherNFA ); nfa.dumpInternalTranstbl();

    // NFA regex_r = NFA.buildNFABasic( new Input( 'r' ) );
    NFA regex_s = NFA.buildNFABasic( new Input( 's' ) );
    NFA regex_t = NFA.buildNFABasic( new Input( 't' ) );

    /// RegEx #0: s|t

    NFA regex_s_OR_t = NFA.buildNFAAlternation( regex_s, regex_t );
    // regex_s_OR_t.dumpInternalTranstbl();

    // // RegEx #1: st

    // NFA regex_rs = NFA.buildNFAConcatenation( regex_r, regex_s );
    // NFA regex_rst = NFA.buildNFAConcatenation( regex_rs, regex_t );
    // regex_rst.dumpInternalTranstbl();

    // // RegEx #2: s*

    // NFA regex_s_STAR = NFA.buildNFAKleeneStar( regex_s );
    // regex_s_STAR.dumpInternalTranstbl();

    // // RegEx #3: s*|ts

    // NFA tsNFA = NFA.buildNFAConcatenation( regex_t, regex_s );
    // NFA regex_s_STAR_or_ts = NFA.buildNFAAlternation( regex_s_STAR, tsNFA );
    // regex_s_STAR_or_ts.dumpInternalTranstbl();

    // RegEx #4: (s|t)*stt

    NFA regex_s_OR_t_STAR = NFA.buildNFAKleeneStar( regex_s_OR_t );
    NFA regex_st = NFA.buildNFAConcatenation( regex_s, regex_t );
    NFA regex_stt = NFA.buildNFAConcatenation( regex_st, regex_t );
    NFA regex_s_OR_t_STAR_stt = NFA.buildNFAConcatenation( regex_s_OR_t_STAR, regex_stt );
    regex_s_OR_t_STAR_stt.dumpInternalTranstbl();

    Set<State> s = new HashSet<>();
    s.add( new State( 6 ) );
    // System.out.println( s = regex_s_OR_t_STAR_stt._epsClosure( s ) );
    // System.out.println( s = regex_s_OR_t_STAR_stt._nextNStates( s, new Input( 's' ) ) );

    DFA dfa = regex_s_OR_t_STAR_stt.dfa();
    dfa.show();
    System.out.println( dfa.simulate( "sststststtstt" ) );
  }
}
