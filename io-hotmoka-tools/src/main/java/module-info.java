/*
Copyright 2021 Fausto Spoto

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

module io.hotmoka.tools {
	exports io.hotmoka.tools;
	requires io.hotmoka.tendermint;
	requires io.hotmoka.service;
	requires io.hotmoka.remote;
	requires io.hotmoka.constants;
	requires io.hotmoka.beans;
	requires io.hotmoka.instrumentation;
	requires io.hotmoka.verification;
	requires io.hotmoka.whitelisting;
	requires io.hotmoka.views;
	requires io.hotmoka.crypto;
	requires info.picocli;
	opens io.hotmoka.tools.internal.cli to info.picocli; // for injecting CLI options
}