package uw.cse.mag.appliancereader.imgproc;

public class Size {

	public final int width;
	public final int height;
	
	/**
	 * Constructs a general interface for defining a size
	 * neither value can be negative
	 * @param w width	
	 * @param h height
	 */
	public Size(int w, int h) {
		width = w;
		height = h;
		checkRep("Constructor");
	}
	
	public String toString(){
		return "[Size, width: " + width+ " height: " +height+ "]";
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (o.getClass() != this.getClass()) return false;
		Size s = (Size)o;
		return s.height == this.height && s.width == this.width;
	}
	
	@Override
	public int hashCode(){
		return 17 * width + 7 * height;
	}
	
	private void checkRep(String preTag) {
		if (width < 0)
			throw new RuntimeException(preTag + ": negative width not allowed");
		if (height < 0)
			throw new RuntimeException(preTag + ": negative height not allowed");
	}
}
