package mytest.infinispan;

import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by ruhan on 3/2/17.
 */
public class BigCacheTest
{
    static int MILLION = 1000000;

    static int K = 1024;

    static int size = 1 * MILLION;

    static int M = 1024 * K;

    static final String aLongString =
                    "/maven/remote/koji-org.jboss.eap-jboss-eap-parent-7.1.0.GA_redhat_6-1/org/jboss/eap/jboss-eap-parent/7.1.0.GA-redhat-6/jboss-eap-parent-7.1.0.GA-redhat-6.pom";

    public static void main( String[] args ) throws Exception
    {

        DefaultCacheManager manager = new DefaultCacheManager( "infinispan.xml" );
        Cache<Object, NfcConcreteResourceWrapper> big = manager.getCache( "big" );

        System.out.println( "\nTest ISPN cache!" );
        test( big );
        big.clear();
        manager.stop();
        Runtime.getRuntime().gc();

        Map<Object, NfcConcreteResourceWrapper> map = new HashMap<>();
        System.out.println( "\nTest hash map!" );
        test( map );

    }

    private static void test( Map<Object, NfcConcreteResourceWrapper> big )
    {
        Runtime r = Runtime.getRuntime();

        long m1 = r.totalMemory() - r.freeMemory();
        System.out.println( "used mem >>>" + m1 );
        List<String> targetKeys = new ArrayList<>();

        for ( int i = 0; i < size; i++ )
        {
            String key = DigestUtils.md5Hex( UUID.randomUUID().toString() );
            if ( i % 10000 == 0 ) // pick up some keys
            {
                targetKeys.add( key );
                big.put( key, new NfcConcreteResourceWrapper( "find it!", 0 ) );
            }
            else
            {
                big.put( key, new NfcConcreteResourceWrapper( aLongString + i,
                                                              System.currentTimeMillis() ) ); // append the path string with the i to force jvm create new string for it
            }
        }

        long m2 = r.totalMemory() - r.freeMemory();
        ;
        System.out.println( "used mem (after) >>>" + m2 );
        System.out.println( "used mem diff (M) >>>" + ( m2 - m1 ) / M );

        System.out.println( "cache.size=" + big.size() );
        System.out.println( "targetKeys.size=" + targetKeys.size() );

        // test retrieval
        long start = System.currentTimeMillis();
        System.out.println( "begin >>>" + start );
        for ( String targetKey : targetKeys )
        {
            NfcConcreteResourceWrapper value = big.get( targetKey );
            //System.out.println("value=" + value.getPath());
            if ( !value.getPath().equals( "find it!" ) )
            {
                throw new RuntimeException( "wrong value: " + value.getPath() );
            }
        }
        long end = System.currentTimeMillis();
        System.out.println( "end >>>" + end );
        System.out.println( "retrieval duration(ms) >>>" + ( end - start ) );

        // test put
        start = System.currentTimeMillis();
        System.out.println( "begin >>>" + start );
        for ( int i = 0; i < 1000; i++ ) // put 1000 times
        {
            big.put( DigestUtils.md5Hex( UUID.randomUUID().toString() ),
                     new NfcConcreteResourceWrapper( "new value " + i, 0 ) );
        }
        end = System.currentTimeMillis();
        System.out.println( "end >>>" + end );
        System.out.println( "insertion duration(ms) >>>" + ( end - start ) );
    }
}

class NfcConcreteResourceWrapper
{
    @Field( index = Index.YES, analyze = Analyze.NO )
    private String location;

    @Field( index = Index.YES, analyze = Analyze.NO )
    private String path;

    @Field
    private long timeout;

    public NfcConcreteResourceWrapper( String resource, long timeout )
    {
        this.path = resource;
        this.timeout = timeout;
    }

    public String getLocation()
    {
        return location;
    }

    public String getPath()
    {
        return path;
    }

    public long getTimeout()
    {
        return timeout;
    }
}