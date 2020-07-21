package fotok;

public enum ERotation {
	rotation0 {
		@Override
		public String getJSClass() {
			return "rotate-0";
		}
	},
	rotation90 {
		@Override
		public String getJSClass() {
			return "rotate-90";
		}
	},
	rotation180 {
		@Override
		public String getJSClass() {
			return "rotate-180";
		}
	},
	rotation270 {
		@Override
		public String getJSClass() {
			return "rotate-270";
		}
	};

	abstract public String getJSClass();
}
