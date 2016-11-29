package queens;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SimpleAchieveREInitiator;
import jade.proto.SubscriptionInitiator;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

@SuppressWarnings("serial")
public class Queen extends Agent{	

	// column number of the chess board on which this queen will operate
	private int column_id; // remains same through out the life time of the agent
	private int max_col;
	private AID pred;
	private AID succ;
	private Position safe_pos = null;
	private ArrayList<Integer> tried_row = new ArrayList<Integer>();
	private int counter = 0;
	private boolean solutionFinalized = false;
	Random randomGenerator = new Random();
	ArrayList<Position> list = new ArrayList<Position>();
	
	
	private ArrayList<Position> positions = new ArrayList<Position>();
	@Override
	protected void setup() {		
		Object[] args = getArguments();
		if(args == null || args.length == 0){
			System.out.println("<" + getLocalName() + "> not invoked with queen id as argument. Exiting...");
			doDelete();
			return;
		}	
		
		column_id = Integer.parseInt((String) args[0]);
		max_col = Integer.parseInt((String) args[1]);		
		
		// Register the queen in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("queen");
		sd.setName(Integer.toString(column_id));
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}		
		System.out.println("<"+getLocalName()+"> is ready !");
		subscribeForPredAndSucc();
		
		SequentialBehaviour sb = new SequentialBehaviour();
		sb.addSubBehaviour(new QueenStatus());
		sb.addSubBehaviour(new PositionSelectorBehavior());
		
		ParallelBehaviour pb = new ParallelBehaviour();
		pb.addSubBehaviour(new FinalPosition());
		sb.addSubBehaviour(new FinalPosition());
		addBehaviour(sb);
	}

	@Override
	protected void takeDown() {
		// TODO Auto-generated method stub
		super.takeDown();
	}
	
	/**
	 * Subscribes for predecessor and successor queen agents. 
	 * Note that first queen will not have any predecessor and Nth queen will not have any successor
	 */
	private void subscribeForPredAndSucc(){
		// Build the description used as template for the subscription
		DFAgentDescription template = new DFAgentDescription();	
		ServiceDescription templateSd_1 = new ServiceDescription();
		templateSd_1.setType("queen");
		template.addServices(templateSd_1);
		
		SearchConstraints sc = new SearchConstraints();
		// We want to receive 10 results at most
		sc.setMaxResults(new Long(10));
		
		
  		
		addBehaviour(new SubscriptionInitiator(this, DFService.createSubscriptionMessage(this, getDefaultDF(), template, sc)) {
			protected void handleInform(ACLMessage inform) {  			
  			try {
					DFAgentDescription[] results = DFService.decodeNotification(inform.getContent());
		  		if (results.length > 0) {		  			
		  			for (int i = 0; i < results.length; ++i) {
		  				DFAgentDescription dfd = results[i];
		  				AID provider = dfd.getName();
		  				Iterator it = dfd.getAllServices();
		  				while (it.hasNext()) {
		  					ServiceDescription sd = (ServiceDescription) it.next();
		  					if (sd.getType().equals("queen")) {	  							
		  						
		  						if (column_id - 1 == Integer.parseInt(sd.getName())){
		  							pred = provider;
		  							System.out.println("<"+getLocalName()+"> found its predecessor: " + pred.getName());
		  						}
		  						
		  						if (column_id + 1 == Integer.parseInt(sd.getName())){
		  							succ = provider;
		  							System.out.println("<"+getLocalName()+"> found its successor: " + succ.getName());
		  						}
		  						
		  					}
		  				}
		  			}
		  		}
		  	}
		  	catch (FIPAException fe) {
		  		fe.printStackTrace();
		  	}
			}
		} );
	}
	
	private class PositionSelectorBehavior extends Behaviour {

		private int step = 0;
		private MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP); // The template to receive replies
		boolean exit = false;
		@Override
		public void action() {
			
			switch(step) {
			
			case 0:
				if (column_id == 0) {
					step = 2;
				} else {
					step =1;
				}
				break;
				
			case 1:	
				positions.clear();
				tried_row.clear();
				ACLMessage reply = myAgent.receive(mt);
				if(reply !=null){					
					if(reply.getPerformative() == ACLMessage.CFP){						
						try {
							positions = (ArrayList<Position>) reply.getContentObject();
							if(positions == null || positions.size() == 0){
								System.out.println("<"+ getLocalName() + "> Ignoring the Inform message with empty positions from predecessor, " + pred);					
							}else{
								//time to find positions for this queen using predecessors' positions
								System.out.println("<"+ getLocalName() + "> Received positions "+ positions.toString() + " from predecessor, " + pred);
								step = 2;
							}
						} catch (UnreadableException e) {							
							e.printStackTrace();
						}
					}
				}else{					
					block();
				}				
				
				break;
			
			case 2:				
				safe_pos = choosePosition();				
				if(safe_pos == null){
					//no safe position found... request predecessor to change and wait for updated positions from predecessors
					if(pred != null){						
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.addReceiver(pred);
						msg.setContent("no_safe_position");
						positions.clear();
						tried_row.clear();
						myAgent.send(msg);
						System.out.println("<"+getLocalName() + " could not find a safe position...requesting predecessor to change");
						mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
						step = 1;
					}else{
						System.out.println("<"+getLocalName()+" Failed to find a solution..");	
						exit = true;
					}
					
				}else {
					System.out.println("<"+getLocalName()+"> found safe position, " + safe_pos.toString());					
					positions.add(safe_pos);
					tried_row.add(safe_pos.getRow());
					if(succ != null){ //inform successor
						ACLMessage msg = new ACLMessage(ACLMessage.CFP);					
						msg.addReceiver(succ);
						try {
							msg.setContentObject(positions);
						} catch (IOException e) {						
							e.printStackTrace();
						}
						myAgent.send(msg);	
						//now wait for either 'no_safe_position' or 'finalize' message
						mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
						step = 3;
					}else {
						//last column, inform predecessor that solution has been found.
						ACLMessage solved = new ACLMessage(ACLMessage.INFORM);
						solved.addReceiver(pred);
						solved.setContent("finalize");
						myAgent.send(solved);
						mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
						solutionFinalized = true;
					}
					
				}
				break;
				
			case 3:
				ACLMessage msg = myAgent.receive(mt);
				if(msg !=null){
					if(msg.getPerformative() == ACLMessage.INFORM ){
						if(msg.getContent().equals("no_safe_position")){
							positions.remove(column_id);
							safe_pos = null;							
							step = 2; // try finding a new safe position
						}else if(msg.getContent().equals("finalize")){
							//solution achieved. Inform predecessor
							if(pred != null){
								ACLMessage m = new ACLMessage(ACLMessage.INFORM);
								m.addReceiver(pred);
								m.setContent("finalize");
								myAgent.send(m);
								System.out.println("<"+getLocalName()+"> informed predecessor to finalize");
							}
							solutionFinalized = true;
							System.out.println("<"+getLocalName()+"> Final position: " + safe_pos.toString());
							mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
						}else{
							System.out.println("<"+getLocalName()+"> received unknown message" + msg.getSender());
						}						
					}
				}else{
					block();
				}
				break;
				
			}
			
		}

		@Override
		public boolean done() {
			if(exit || solutionFinalized ){
				System.out.println("<"+getLocalName()+"> exiting...");
				return true;
			}else
				return false;
		}
		
	}
	
	private class QueenStatus extends Behaviour {
		
		boolean done = false;
		@Override
		public void action() {
			
			if(column_id == 0 && succ !=null){
				done = true;
			}else if(column_id == max_col && pred !=null){
				done = true;
			}else if(pred !=null && succ != null) {
				done = true;
			}			
		}

		@Override
		public boolean done() {			
			return done;
		}		
	}
	
	private class FinalPosition extends CyclicBehaviour {
		
		private MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST); 
		
		@Override
		public void action() {
			
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null){
				if(msg.getContent().equals("getFinalPosition")){
					ACLMessage rep = msg.createReply();
					try {
						rep.setPerformative(ACLMessage.INFORM);
						rep.setContentObject(safe_pos);
						myAgent.send(rep);
					} catch (IOException e) {						
						e.printStackTrace();
					}
				}
			}else {
				block();
			}			
		}
		
	}
	
	/**
	 * Checks if two positions are attacking to each other by calculating the slope between two positions
	 * @param p1
	 * @param p2
	 * @return true, if yes. Else false
	 */
	private boolean isAttacking(Position p1, Position p2){
		if((p1.getRow() == p2.getRow()) || (p1.getColumn() == p2.getColumn())){
			return true;
		}
		float slope = ((float)(p2.getColumn() - p1.getColumn())) / (p2.getRow() - p1.getRow());
		
		if(slope == 1.0f || slope == -1.0f){
			return true;
		}else{
			return false;
		}
	}
	
	private Position choosePosition() {
		// choose one position in the column and check if it is attacked by any predecessors		
		if( column_id == 0){
			if(counter > max_col){
				System.out.println("all rows used in column 0");
				return null;
			}
			//no positions from predecessor, so free to choose any position
			Position pos = new Position(counter, column_id);			
			counter ++;
			return pos;			
		}
		
		for(int i = 0 ; i<= max_col; ++i){
			Position pos = new Position(i, column_id);
			boolean attacking = false;
			for(Position position: positions){
										
				if(isAttacking(position, pos)){
					attacking = true;
					break;
				}
			}
			if(!attacking && !tried_row.contains(i)){
				//found safe position
				return pos;
			}
		}
		
		return null;
		
	}		
	

}





