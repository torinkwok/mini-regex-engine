import java.util.*;

public class NFA implements Cloneable
{
  public int start;
  public int end;

  public final Vector<Object> transtbl;

  @Override
  public NFA clone() { return new NFA( this ); }

  public NFA( NFA src )
  {
    Vector<Object> cloned_transtbl = new Vector<Object>( src.count() );
    for ( int i = 0; i < src.count(); i++ )
    {
      Vector<Input> src_r = ( Vector<Input> )src.transtbl.get( i );
      Vector<Input> cloned_r = new Vector<Input>( src.count() );
      for ( int j = 0; j < src.count(); j++ )
      {
        cloned_r.add( src_r.get( j ) );
      }
      cloned_transtbl.add( cloned_r );
    }

    this.transtbl = cloned_transtbl;
    this.start    = src.start;
    this.end      = src.end;
  }

  public NFA( int size, int start, int end )
  {
    assert( isLegalState( start ) );
    assert( isLegalState( end ) );

    transtbl = new Vector<Object>( size );

    this.start = start;
    this.end   = end;

    // Initialize transtbl with an "empty graph",
    // no transitions between its states

    for ( int i = 0; i < size; i++ )
    {
      Vector<Input> row = new Vector<Input>( size );
      for ( int j = 0; j < size; j++ ) { row.add( Input.NONE ); }

      transtbl.add( row );
    }
  }

  public int     count()               { return transtbl.size();      }
  public boolean isLegalState( int s ) { return s >= 0 || s < count(); }

  public void addTransition( int from, int to, Input in )
  {
    assert( isLegalState( from ) );
    assert( isLegalState( to ) );

    Vector<Input> row = ( Vector<Input> )transtbl.get( from );
    row.setElementAt( in, to );
  }

  public void show()
  {
    System.out.println( String.format( "This NFA got %d states: s0 - s%d", count(), count() - 1 ) );
    System.out.println( String.format( "The initial state is s%d", start ) );
    System.out.println( String.format( "The final state is s%d", end ) );

    for ( int from = 0; from < count(); from++ )
    {
      for ( int to = 0; to < count(); to++ )
      {
        Vector<Input> row = ( Vector<Input> )transtbl.get( from );
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
  public void appendEmptyState()
  {
    Vector<Input> newrow = new Vector<Input>( count() + 1 );
    for ( int i = 0; i < count(); i++ ) { newrow.add( Input.NONE ); }

    transtbl.add( newrow );

    for ( int i = 0; i < count(); i++ )
    {
      Vector<Input> ri = ( Vector<Input> )transtbl.get( i );
      ri.add( Input.NONE );
    }

    this.end++;
  }

  /** Renames all the NFA's states:
   *
   *  For each NFA state: number += shift. Functionally, this
   *  doesn't affect the NFA, it only makes it larger and renames
   *  its states.
   */
  public void shiftStates( int shift )
  {
    if ( shift < 1 ) { return; }

    for ( int i = 0; i < shift; i++ )
    {
      Vector<Input> newrow = new Vector<Input>( count() + shift );
      for ( int j = 0; j < count(); j++ ) { newrow.add( Input.NONE ); }

      transtbl.insertElementAt( newrow, 0 );
    }

    for ( int i = 0; i < count(); i++ )
    {
      Vector<Input> ri = ( Vector<Input> )transtbl.get( i );
      for ( int j = 0; j < shift; j++ ) { ri.insertElementAt( Input.NONE, 0 ); }
    }

    this.start += shift;
    this.end   += shift;
  }

  /** Fills states 0 up to src.count() with src's states.
   */
  public void fillStates( NFA nfa )
  {
    NFA src = nfa.clone();
    NFA dst = this;
    int srcsz = src.count();

    for ( int i = 0; i < srcsz; i++ )
    {
      for ( int j = 0; j < srcsz; j++ )
      {
        Vector<Input> rsrc = ( Vector<Input> )src.transtbl.get( i );
        Vector<Input> rdst = ( Vector<Input> )dst.transtbl.get( i );

        rdst.setElementAt( rsrc.get( j ), j );
      }
    }
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
      Vector<Input> r = ( Vector<Input> )transtbl.get( i );
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

    nfa1.shiftStates( 1 );            // make room for the new initial state
    nfa2.shiftStates( nfa1.count() ); // make room for nfa1's states

    NFA nfaUnion = new NFA( nfa2 );   // create a new nfa and initialize it
    nfaUnion.fillStates( nfa1 );      // nfa1's states take their places in the new nfa

    nfaUnion.addTransition( 0, nfa1.start, Input.EPS );
    nfaUnion.addTransition( 0, nfa2.start, Input.EPS );
    nfaUnion.start = 0;

    nfaUnion.appendEmptyState();
    nfaUnion.end = nfaUnion.count() - 1;

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
    nfa2.shiftStates( nfa1.count() - 1 );

    NFA nfaConcat = new NFA( nfa2 );

    // nfa1's states take their places in nfaConcat NOTE: nfa1's
    // final state overwrites nfa2's initial state, thus we get
    // the desired merge automagically (the transition from
    // nfa2's initial state now transits from nfa1's final state)
    // 
    nfaConcat.fillStates( nfa1 );

    // Set the new initial state (the final state stays nfa2's
    // final state, and was already copied)
    //
    nfaConcat.start = nfa1.start;

    return nfaConcat;
  }

  public static NFA buildNFAKleeneStar( NFA n )
  {
    NFA nfa = n.clone();

    nfa.shiftStates( 1 );

    NFA nfaKleeneStar = new NFA( nfa );
    nfaKleeneStar.fillStates( nfa );
    nfaKleeneStar.appendEmptyState();

    nfaKleeneStar.start = 0;
    nfaKleeneStar.end = nfaKleeneStar.count() - 1;

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
  public Set<Integer> _epsClosure( Set<Integer> T )
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

    Set<Integer> closure = new HashSet<>();
    if ( T.isEmpty() ) return closure;

    AdHocStack<Integer> stack = new AdHocStack<>();

    for ( int t : T )
    {
      stack.push( t );

      while ( !stack.isEmpty() )
      {
        t = stack.pop();

        Vector<Input> r = ( Vector<Input> ) transtbl.get( t );
        Set<Integer> U = new HashSet<>();

        U.add( t );

        for ( int c = 0; c < count(); c++ )
          if ( r.get( c ) == Input.EPS )
            U.add( c );

        for ( int u : U )
        {
          if ( !closure.contains( u ) )
            {
              closure.add( u );
              stack.push( u );
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
  public Set<Integer> _move( Set<Integer> T, Input A )
  {
    // The function traverses the set T, and looks for
    // transitions on the given input, returning the states that
    // can be reached. It doesn't take into account the *eps*
    // transitions from those states - there's eps-closure
    // algorithm for that.

    Set<Integer> states = new HashSet<>();

    if ( A == Input.EPS || A == Input.NONE )
      return states;

    for ( int t : T )
    {
      Vector<Input> r = ( Vector<Input> )transtbl.get( t );
      for ( int c = 0; c < count(); c++ )
      {
        Input in = r.get( c );
        if ( in == Input.EPS || in == Input.NONE ) continue;
        if ( in.equals( A ) )                      states.add( c );
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

    nfa.appendEmptyState(); nfa.dumpInternalTranstbl();
    nfa.shiftStates( 3 );   nfa.dumpInternalTranstbl();

    NFA anotherNFA = new NFA( 4, 0, 3 );

    anotherNFA.addTransition( 0, 1, new Input( 'a' ) );
    anotherNFA.addTransition( 0, 0, new Input( 'b' ) );
    anotherNFA.addTransition( 1, 2, new Input( 'b' ) );
    anotherNFA.addTransition( 2, 3, new Input( 'b' ) );
    anotherNFA.addTransition( 2, 1, new Input( 'a' ) );
    anotherNFA.addTransition( 3, 1, new Input( 'a' ) );

    anotherNFA.dumpInternalTranstbl();
    nfa.fillStates( anotherNFA ); nfa.dumpInternalTranstbl();

    NFA regex_r = NFA.buildNFABasic( new Input( 'r' ) );
    NFA regex_s = NFA.buildNFABasic( new Input( 's' ) );
    NFA regex_t = NFA.buildNFABasic( new Input( 't' ) );

    /// RegEx #0: s|t

    NFA regex_s_OR_t = NFA.buildNFAAlternation( regex_s, regex_t );
    regex_s_OR_t.dumpInternalTranstbl();

    // RegEx #1: st

    NFA regex_rs = NFA.buildNFAConcatenation( regex_r, regex_s );
    NFA regex_rst = NFA.buildNFAConcatenation( regex_rs, regex_t );
    regex_rst.dumpInternalTranstbl();

    // RegEx #2: s*

    NFA regex_s_STAR = NFA.buildNFAKleeneStar( regex_s );
    regex_s_STAR.dumpInternalTranstbl();

    // RegEx #3: s*|ts

    NFA tsNFA = NFA.buildNFAConcatenation( regex_t, regex_s );
    NFA regex_s_STAR_or_ts = NFA.buildNFAAlternation( regex_s_STAR, tsNFA );
    regex_s_STAR_or_ts.dumpInternalTranstbl();

    // RegEx #4: (s|t)*stt

    NFA regex_s_OR_t_STAR = NFA.buildNFAKleeneStar( regex_s_OR_t );
    NFA regex_st = NFA.buildNFAConcatenation( regex_s, regex_t );
    NFA regex_stt = NFA.buildNFAConcatenation( regex_st, regex_t );
    NFA regex_s_OR_t_STAR_stt = NFA.buildNFAConcatenation( regex_s_OR_t_STAR, regex_stt );
    regex_s_OR_t_STAR_stt.dumpInternalTranstbl();

    Set<Integer> s = new HashSet<>();
    s.add( 6 );
    System.out.println( s = regex_s_OR_t_STAR_stt._epsClosure( s ) );
    System.out.println( s = regex_s_OR_t_STAR_stt._move( s, new Input( 's' ) ) );
  }
}
