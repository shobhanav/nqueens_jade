package queens;

import jade.wrapper.StaleProxyException;

public class Testing {

	public static void main(String[] args) throws StaleProxyException {
		
		TestMain controller;
		try {
			controller = TestMain.getInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//TestMain.killInstance();
	}
}
