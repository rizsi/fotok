package fotok;

public enum ESize {
	normal {
		@Override
		public int reqSize() {
			return 1280;
		}
	},
	thumb {
		@Override
		public int reqSize() {
			return 320;
		}
	},
	/** Not actual image but a file containing the size of the image */
	size {
		@Override
		public int reqSize() {
			// TODO Auto-generated method stub
			return 0;
		}
	};

	abstract public int reqSize();
}
