package io.devjoy.gitea.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Generated;

public class Command {
	String executable;
	List<String> args;
	List<Option> options;
	
	@Generated("SparkTools")
	private Command(Builder builder) {
		this.executable = builder.executable;
		this.args = builder.args;
		this.options = builder.options;
	}
	@Generated("SparkTools")
	public static Builder builder() {
		return new Builder();
	}
	@Generated("SparkTools")
	public static final class Builder {
		private String executable;
		private List<String> args = new ArrayList<>();
		private List<Option> options = new ArrayList<>();

		private Builder() {
		}

		public Builder withExecutable(String executable) {
			this.executable = executable;
			return this;
		}

		public Builder withArgs(List<String> args) {
			this.args = args;
			return this;
		}

		public Builder withOptions(List<Option> options) {
			this.options = options;
			return this;
		}
		
		public Builder addArg(String arg) {
			this.args.add(arg);
			return this;
		}
		
		public Builder addArgs(Collection<String> args) {
			this.args.addAll(args);
			return this;
		}
		
		public Builder addOption(Option option) {
			this.options.add(option);
			return this;
		}
		
		public Builder addOptions(Collection<Option> options) {
			this.options.addAll(options);
			return this;
		}
		
		public Command build() {
			return new Command(this);
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s %s %s", executable, args.stream().collect(Collectors.joining(" ")), options.stream().map(Option::toString).collect(Collectors.joining(" ")));
	}
	
	public String[] toArray() {
		List<String> cmd = new ArrayList<>();
		
		cmd.add(executable);
		cmd.addAll(args);
		cmd.addAll(options.stream()
				.flatMap(o -> Arrays.stream(o.toArray()))
				.collect(Collectors.toList()));
		return cmd.toArray(new String[] {});
	}
}
