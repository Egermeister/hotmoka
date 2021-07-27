package io.takamaka.code.tokens;

/*
Copyright 2021 Filippo Fantinato and Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import static io.takamaka.code.lang.Takamaka.event;
import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;
import java.util.function.Supplier;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageMapView;
import io.takamaka.code.util.StorageSet;
import io.takamaka.code.util.StorageTreeMap;
import io.takamaka.code.util.StorageTreeSet;

/**
 * Implementation of the ERC721 standard for non-fungible tokens.
 * This code has been inspired by OpenZeppelin's implementation in Solidity.
 * 
 * @see <a href="https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/token/ERC721/ERC721.sol>ERC721.sol</a>.
 */
public class ERC721 extends Contract implements IERC721 {

	/**
	 * The name of this token collection.
	 */
	private final String name;

	/**
	 * The symbol of this token collection.
	 */
	private final String symbol;

	/**
	 * True if and only if events get generated by this token collection.
	 */
	private final boolean generateEvents;

	/**
	 * A map from each token identifier to its owner.
	 */
	private final StorageMap<BigInteger, Contract> owners = new StorageTreeMap<>();

	/**
	 * A map from each owner to the number of tokens that it owns.
	 */
	private final StorageMap<Contract, BigInteger> balances = new StorageTreeMap<>();

	/**
	 * A map from each token identifier to a contract that has been approved
	 * for transferring the token: only that contract or the owner of the token
	 * or the approved operators of the owner can transfer the token.
	 */
	private final StorageMap<BigInteger, Contract> tokenApprovals = new StorageTreeMap<>();

	/**
	 * A map from each owner to the set of contracts that it has approved for
	 * transferring all its tokens.
	 */
	private final StorageMap<Contract, StorageSet<Contract>> operatorApprovals = new StorageTreeMap<>();

	/**
	 * Builds a collection of non-fungible tokens that does not generate events.
	 * 
	 * @param name the name of the collection
	 * @param symbol the symbol of the collection
	 */
	public ERC721(String name, String symbol) {
		this(name, symbol, false);
	}

	/**
	 * Builds a collection of non-fungible tokens.
	 * 
	 * @param name the name of the collection
	 * @param symbol the symbol of the collection
	 * @param generateEvents true if and only if the collection generates events
	 */
	public ERC721(String name, String symbol, boolean generateEvents) {
		this.name = name;
		this.symbol = symbol;
		this.generateEvents = generateEvents;
	}

	/**
	 * Yields the token collection name.
	 */
	@View
	public final String name() {
		return name;
	}

	/**
	 * Yields the token collection symbol.
	 */
	@View
	public final String symbol() {
		return symbol;
	}

	@Override @FromContract
	public void transferFrom(Contract from, Contract to, BigInteger tokenId) {
		require(_isApprovedOrOwner(caller(), tokenId), "transfer caller is not owner nor approved");
		require(to instanceof ExternallyOwnedAccount || to instanceof IERC721Receiver,
			"transfer destination must be an externally owned account or implement IERC721Receiver");
		_transfer(from, to, tokenId);
	}

	/**
	 * Called before a token gets transferred. Subclasses might add specific code here.
	 * 
	 * @param from the previous owner of the token
	 * @param to the new owner of the token
	 * @param tokenId the identifier of the token
	 */
	protected void beforeTokenTransfer(Contract from, Contract to, BigInteger tokenId) { }

	/**
	 * Called to transfer a token.
	 * 
	 * @param from the previous owner of the token
	 * @param to the new owner of the token
	 * @param tokenId the token identifier
	 */
	protected void _transfer(Contract from, Contract to, BigInteger tokenId) {
		require(ownerOf(tokenId) == from, "transfer of token that is not own");
		require(to != null, "transfer to {@code null}");

		beforeTokenTransfer(from, to, tokenId);
		clearApproval(tokenId);
		balances.put(from, balanceOf(from).subtract(BigInteger.ONE));
		balances.put(to, balanceOf(to).add(BigInteger.ONE));
		owners.put(tokenId, to);

		if (to instanceof IERC721Receiver)
			((IERC721Receiver) to).onERC721Received(from, to, tokenId);

		if (generateEvents)
			event(new Transfer(from, to, tokenId));
	}

	@Override @FromContract
	public void approve(Contract to, BigInteger tokenId) {
		Contract owner = ownerOf(tokenId);
		Contract caller = caller();
		require(owner != to, "approval to current owner");
		require(caller == owner || isApprovedForAll(owner, caller), "the caller is not owner nor approved for all");

		if (to == null)
			tokenApprovals.remove(to);
		else
			tokenApprovals.put(tokenId, to);

		if (generateEvents)
			event(new Approval(owner, to, tokenId));
	}

	private void clearApproval(BigInteger tokenId) {
		tokenApprovals.remove(tokenId);
		if (generateEvents)
			event(new Approval(ownerOf(tokenId), null, tokenId));
	}

	@Override @FromContract
	public void setApprovalForAll(Contract operator, boolean approved) {
		Contract caller = caller();
		require(operator != caller, "the caller cannot approve itself");

		StorageSet<Contract> operators = operatorApprovals.computeIfAbsent(caller, (Supplier<StorageTreeSet<Contract>>) StorageTreeSet::new);
		if (approved)
			operators.add(operator);
		else
			operators.remove(operator);

		if (generateEvents)
			event(new ApprovalForAll(caller, operator, approved));
	}

	@Override @View
	public Contract getApproved(BigInteger tokenId) {
		require(_exists(tokenId), "approved query for non-existent token");
		return tokenApprovals.get(tokenId);
	}

	@Override @View
	public final boolean isApprovedForAll(Contract owner, Contract operator) {
		StorageSet<Contract> approvedForAll = operatorApprovals.get(owner);
		return approvedForAll != null && approvedForAll.contains(operator);
	}

	/**
	 * Determines if {@code spender} can transfer the given token, since it is
	 * its owner or has been approved by its owner.
	 * 
	 * @param spender the contract that should transfer the token
	 * @param tokenId the identifier of the token
	 * @return true if and only if that condition holds
	 */
	protected boolean _isApprovedOrOwner(Contract spender, BigInteger tokenId) {
		require(_exists(tokenId), "query for non-existent token");
		Contract owner = ownerOf(tokenId);

		return spender == owner || getApproved(tokenId) == spender || isApprovedForAll(owner, spender);
	}

	/**
	 * Mints a new token. If {@code to} is a {@link IERC721Receiver}, its
	 * {@link IERC721Receiver#onERC721Received(Contract, Contract, BigInteger)} method gets invoked.
	 * 
	 * @param to the owner of the new token. This must be an externally owned account
	 *           or implement {@link IERC721Receiver}
	 * @param tokenId the identifier of the new token; this must not exist already
	 */
	protected void _mint(Contract to, BigInteger tokenId) {
		require(!_exists(tokenId), "token already minted");
		require(to instanceof ExternallyOwnedAccount || to instanceof IERC721Receiver,
			"mint destination must be an externally owned account or implement IERC721Receiver");

		beforeTokenTransfer(null, to, tokenId);
		balances.put(to, balanceOf(to).add(BigInteger.ONE));
		owners.put(tokenId, to);

		if (to instanceof IERC721Receiver)
			((IERC721Receiver) to).onERC721Received(null, to, tokenId);

		if (generateEvents)
			event(new Transfer(null, to, tokenId));
	}

	/**
	 * Returns the Uniform Resource Identifier (URI) for token {@code tokenId}.
	 * 
	 * @param tokenId the token whose URI must be returned
	 */
	@View
	public String tokenURI(BigInteger tokenId) {
		require(_exists(tokenId), "URI query for non-existent token");

		String baseURI = _baseURI();
		return !baseURI.isEmpty() ? (baseURI + tokenId) : "";
	}

	/**
	 * Yields the base form the URI for the tokens of this contract.
	 * Subclasses can specify their base URI here.
	 * 
	 * @return the base form of the URI
	 */
	@View
	protected String _baseURI() {
		return "";
	}

	@Override @View
	public BigInteger balanceOf(Contract owner) {
		require(owner != null, "balance query for null");
		return balances.getOrDefault(owner, BigInteger.ZERO);
	}

	@Override @View
	public Contract ownerOf(BigInteger tokenId) {
		return ownerOf(tokenId, owners);
	}

	private Contract ownerOf(BigInteger tokenId, StorageMapView<BigInteger, Contract> owners) {
		Contract owner = owners.get(tokenId);
		require(owner != null, "non-existent token");
		return owner;
	}

	@Exported
	protected class ERC721Snapshot extends Storage implements IERC721View {
		private final StorageMapView<BigInteger, Contract> owners = ERC721.this.owners.snapshot();
		private final StorageMapView<Contract, BigInteger> balances = ERC721.this.balances.snapshot();

		@Override @View
		public BigInteger balanceOf(Contract owner) {
			return balances.getOrDefault(owner, BigInteger.ZERO);
		}

		@Override @View
		public Contract ownerOf(BigInteger tokenId) {
			return ERC721.this.ownerOf(tokenId, owners);
		}

		@Override @View
		public IERC721View snapshot() {
			return this;
		}
	}

	@Override @View
	public IERC721View snapshot() {
		return new ERC721Snapshot();
	}

	/**
	 * Burns a token.
	 * 
	 * @param tokenId the identifier of the token to burn. This must already exist
	 */
	protected void _burn(BigInteger tokenId) {
		Contract owner = ownerOf(tokenId);
		beforeTokenTransfer(owner, null, tokenId);
		clearApproval(tokenId);

		balances.put(owner, balanceOf(owner).subtract(BigInteger.ONE));
		owners.remove(tokenId);
		if (generateEvents)
			event(new Transfer(owner, null, tokenId));
	}

	@View
	protected final boolean _exists(BigInteger tokenId) {
		return owners.containsKey(tokenId);
	}
}