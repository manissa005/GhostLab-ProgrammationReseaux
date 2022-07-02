package serveur;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Format {

	private static int[] FormatSize( Object[] l ) {
		int endian = 2; // 1 = little 2 = big;
		int m = 0; for( int i = 0; i < l.length; i++ ) {
			Object o = l[i];
			if( o instanceof String ) {
				m += ( (String) o ).length();
			} else if( o instanceof UpletByte ) {
				UpletByte nombre = (UpletByte) o;
				if( nombre.getX() < 0 ) continue;
				m += nombre.getOctet();
			} if( i < l.length - 2 ) {
				endian = 1;
				m++;
			}
		} return new int[]{ endian, m };
	}

	public static byte[] FormatByte( Object... l ) {
		int[] info = FormatSize( l );
		ByteBuffer data = ByteBuffer.allocate( info[1] );
		data.order( ByteOrder.BIG_ENDIAN );
		for( int i = 0; i < l.length; i++ ) {
			Object o = l[i];
			if( o instanceof String ) {
				data.put( ( (String) o ).getBytes() );
			} else if( o instanceof UpletByte ) {
				UpletByte n = (UpletByte) o;
				if( n.getOctet() == 1 ) {
					data.put( (byte) n.getX() );
				} else if( n.getOctet() == 2 ) {
					data.put( Format2Bytes( n.getX() ) );
				}
			}
			if( i < l.length - 2 )
				data.put( (byte) ' ' );
		}
		return data.array();
	}


	static byte[] Format2Bytes( int x ) {
		return new byte[]{
				  (byte) ( ( x >>> 8 ) & 0xFF ),
				  (byte) ( x & 0xFF )
		};
	}


}
