package takamaka.tests.crowdfunding;
import java.math.BigInteger;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;
import takamaka.lang.PayableContract;
import takamaka.lang.Storage;
import takamaka.util.StorageList;

public class CrowdFundingSimplified extends Contract {
	public Campaign newCampaign(PayableContract beneficiary, BigInteger goal) {
		return new Campaign(beneficiary, goal);
	}

	public @Payable @Entry void contribute(BigInteger amount, Campaign campaign) {
		campaign.funders.add(new Funder(caller(), amount));
		campaign.amount = campaign.amount.add(amount);
	}

	public boolean checkGoalReached(Campaign campaign) {
		if (campaign.amount.compareTo(campaign.fundingGoal) < 0)
			return false;
		else {
			BigInteger amount = campaign.amount;
			campaign.amount = BigInteger.ZERO;
			campaign.beneficiary.receive(amount);
			return true;
		}
	}

	public static class Campaign extends Storage {
		private final PayableContract beneficiary;
		private final BigInteger fundingGoal;
		private final StorageList<Funder> funders = new StorageList<>();
		private BigInteger amount;

		private Campaign(PayableContract beneficiary, BigInteger fundingGoal) {
			this.beneficiary = beneficiary;
			this.fundingGoal = fundingGoal;
			this.amount = BigInteger.ZERO;
		}
	}

	private static class Funder extends Storage {
		@SuppressWarnings("unused")
		private final Contract who;
		@SuppressWarnings("unused")
		private final BigInteger amount;

		public Funder(Contract who, BigInteger amount) {
			this.who = who;
			this.amount = amount;
		}
	}
}